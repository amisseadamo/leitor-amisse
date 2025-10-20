// Local: MainActivity.java
package com.example.musicplayera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.Player;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.acrcloud.rec.ACRCloudClient;
import com.acrcloud.rec.ACRCloudConfig;
import com.acrcloud.rec.ACRCloudResult;
import com.acrcloud.rec.IACRCloudListener;
import com.example.musicplayera.Adapter.SongAdapter;
import com.example.musicplayera.Model.Song;
import com.example.musicplayera.presenter.Presenter;
import com.example.musicplayera.view.ContratoView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements ContratoView, SongAdapter.OnSongInteractionListener, IACRCloudListener {

    private Presenter presenter;
    private SongAdapter songAdapter;
    private List<Song> todasAsMusicas = new ArrayList<>();
    private ImageView imgAlbumArt;
    private TextView textTitle, textArtist, textCurrentTime, textTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnShuffle, btnRepeat;
    private ACRCloudClient acrCloudClient;
    

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_BIBLIOTECA = 101;
    private static final int REQUEST_CODE_PLAYLISTS = 102;
    private static final int REQUEST_CODE_RECORD_AUDIO = 103;
    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean isEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Se você estiver usando View Binding, mantenha o seu código.
        // Se não, o setContentView está correto.
        setContentView(R.layout.activity_main);

        // Chama o método que inicializa todas as suas views e, mais importante, o Adapter.
        inicializarViews();

        // O resto da sua inicialização
        setupClickListeners();
        configurarToolbar();
        configurarClickListeners(); // Você parece ter dois métodos de click listener, talvez possa unificar?
        configurarSeekBar();

        // Inicialização do Presenter e do ACRCloud
        presenter = new Presenter(this, this);
        initializeACRCloud();

        // Pede as permissões, o que eventualmente vai chamar presenter.carregarMusicasIniciais()
        verificarPermissoes();
    }

    private void inicializarViews() {
        imgAlbumArt = findViewById(R.id.img_album_art);
        textTitle = findViewById(R.id.text_title);
        textArtist = findViewById(R.id.text_artist);
        textCurrentTime = findViewById(R.id.text_current_time);
        textTotalTime = findViewById(R.id.text_total_time);
        seekBar = findViewById(R.id.seek_bar);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnRepeat = findViewById(R.id.btn_repeat);
        RecyclerView recyclerSongs = findViewById(R.id.recyclerSongs);
        recyclerSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(new ArrayList<>(), this);
        recyclerSongs.setAdapter(songAdapter);
    }

    private void verificarPermissoes() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, REQUEST_CODE_PERMISSIONS);
        } else {
            presenter.carregarMusicasIniciais();
        }
    }
// Adicione este método à sua classe MainActivity

    private void setupClickListeners() {
        // Encontre seus botões pelos IDs que você definiu no XML.
        // Substitua 'btnPlayPause', 'btnNext', 'btnPrev' pelos seus IDs corretos.

        // Exemplo de como encontrar os botões (você pode já estar fazendo isso em 'inicializarViews')
        ImageView btnPlayPause = findViewById(R.id.btn_play_pause); // Use o seu ID
        ImageView btnNext = findViewById(R.id.btn_next);       // Use o seu ID
        ImageView btnPrev = findViewById(R.id.btn_previous);       // Use o seu ID
        ImageView btnRecognize = findViewById(R.id.action_recognize); // Use o seu ID do botão de reconhecimento

        // Configura o clique para o botão Play/Pause
        if (btnPlayPause != null) {
            btnPlayPause.setOnClickListener(v -> {
                if (presenter != null) {
                    presenter.tocarOuPausar();
                }
            });
        }

        // Configura o clique para o botão Próxima
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if (presenter != null) {
                    presenter.proximaMusica();
                }
            });
        }

        // Configura o clique para o botão Anterior
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (presenter != null) {
                    presenter.musicaAnterior();
                }
            });
        }

        // Configura o clique para o botão de Reconhecimento de Música
        if (btnRecognize != null) {
            btnRecognize.setOnClickListener(v -> {
                if (presenter != null) {
                    // Primeiro, verifica a permissão de gravar áudio
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        // Se não tem, pede a permissão. Use um código de requisição diferente.
                        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 200); // Ex: 200 para áudio
                    } else {
                        // Se já tem a permissão, inicia o reconhecimento.
                        presenter.iniciarReconhecimentoMusica();
                    }
                }
            });
        }

 if (btnShuffle != null) {
        btnShuffle.setOnClickListener(v -> {
            if (presenter != null) {
                presenter.alternarModoShuffle();
            }
        });
    }

    if (btnRepeat != null) {
        btnRepeat.setOnClickListener(v -> {
            if (presenter != null) {
                presenter.alternarModoRepeticao();
            }
        });
    }
    }
    private void startAlbumRotation() {
        if (imgAlbumArt == null) return;

        // Cancela qualquer animação anterior para evitar sobreposição
        imgAlbumArt.animate().cancel();

        // Configura e inicia a nova animação
        imgAlbumArt.animate()
                .rotationBy(360f) // Gira 360 graus a partir da posição atual
                .setDuration(10000) // Define a duração de uma rotação completa (10 segundos)
                .setInterpolator(new android.view.animation.LinearInterpolator()) // Mantém a velocidade constante
                .withEndAction(() -> {
                    // Quando a animação termina, se a música ainda estiver tocando,
                    // chama o método novamente para criar um loop infinito.
                    if (presenter != null && presenter.estaTocando()) {
                        startAlbumRotation();
                    }
                })
                .start();
    }
    private void stopAlbumRotation() {
        if (imgAlbumArt != null) {
            imgAlbumArt.animate().cancel();
        }
    }
    public void onStopTrackingTouch(SeekBar s) {
        presenter.avancarPara((long) s.getProgress()); // 👈 Cast para long
    }

    //region Métodos da View (ContratoView)
    @Override
    public void onServiceReady() {
        if (presenter != null) presenter.carregarMusicasIniciais();
    }

    @Override
    public void atualizarListaMusicas(List<Song> musicas) {
        this.todasAsMusicas = musicas;
        runOnUiThread(() -> songAdapter.updateSongs(musicas));
    }

    @Override
    public void musicaMudou(Song song) {
        runOnUiThread(() -> {
            if (song != null) {
                textTitle.setText(song.title);
                textArtist.setText(song.artist);
            }
        });
    }
// ESTA É A CORREÇÃO FINAL PARA FAZER O CLIQUE FUNCIONAR

    @Override
    public void onSongClicked(Song song, int position) {
        if (presenter != null) {
            Log.d("MainActivity", "onSongClicked - Posição: " + position + ", Título: " + song.title);
            // Delega o comando para o Presenter, que sabe o que fazer.
            presenter.tocarTodasAsMusicasNaPosicao(position);
        } else {
            Log.e("MainActivity", "onSongClicked - Presenter é nulo.");
        }
    }

    @Override
    public void atualizarProgresso(long pos, long dur) {
        runOnUiThread(() -> {
            if (dur > 0) {
                seekBar.setMax((int) dur);
                seekBar.setProgress((int) pos);
                textCurrentTime.setText(formatarTempo(pos));
                textTotalTime.setText(formatarTempo(dur));
            }
        });
    }

    // Em MainActivity.java

    @Override
    public void estadoReproducaoMudou(boolean isPlaying) {
        runOnUiThread(() -> {
            // 1. Atualiza o ícone do botão play/pause (você já tem isso)
            btnPlayPause.setImageResource(isPlaying ? R.drawable.pause : R.drawable.play);

            // ================== A CORREÇÃO ESTÁ AQUI ==================
            // 2. Controla a animação da capa do álbum
            if (isPlaying) {
                startAlbumRotation(); // Inicia a animação se a música está tocando
            } else {
                stopAlbumRotation();  // Para a animação se a música foi pausada ou parou
            }
            // ==========================================================
        });
    }


    // Em MainActivity.java (seu projeto com problema)
// Não se esqueça de importar o que for necessário (ex: android.graphics.PorterDuff)

// ... outros métodos da sua classe ...

    @Override
    public void modoEmbaralharMudou(boolean isEnabled) {
        // Garante que a atualização da UI ocorra na thread principal
        runOnUiThread(() -> {
            ImageView btnShuffle = findViewById(R.id.btn_shuffle); // Use o ID correto do seu botão
            if (btnShuffle != null) {
                // Define a cor do ícone com base no estado 'enabled'
                // Usa a cor 'accent_color' que você já definiu em colors.xml
                int color = isEnabled
                        ? ContextCompat.getColor(this, R.color.accent_color)
                        : ContextCompat.getColor(this, android.R.color.white); // Branco para desligado

                // Aplica o filtro de cor. É ASSIM que se muda a cor de um ImageView.
                btnShuffle.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        });
    }


    @Override
    public void modoRepetirMudou(int repeatMode) {
        // Garante que a atualização da UI ocorra na thread principal
        runOnUiThread(() -> {
            ImageView btnRepeat = findViewById(R.id.btn_repeat); // Use o ID correto do seu botão
            if (btnRepeat != null) {

                // A verificação do 'player' foi REMOVIDA.
                // A MainActivity só se preocupa com o 'repeatMode' que ela recebeu.

                if (repeatMode == Player.REPEAT_MODE_OFF) {
                    // Modo DESLIGADO
                    btnRepeat.setImageResource(R.drawable.repeat); // Ícone padrão
                    btnRepeat.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);

                } else if (repeatMode == Player.REPEAT_MODE_ONE) {
                    // Modo REPETIR UMA
                    btnRepeat.setImageResource(R.drawable.repeat); // Precisa ter este ícone!
                    btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.accent_color), PorterDuff.Mode.SRC_IN);

                } else { // Corresponde a Player.REPEAT_MODE_ALL
                    // Modo REPETIR TODAS
                    btnRepeat.setImageResource(R.drawable.repeat); // Ícone padrão
                    btnRepeat.setColorFilter(ContextCompat.getColor(this, R.color.accent_color), PorterDuff.Mode.SRC_IN);
                }
            }
        });
    }

// Em MainActivity.java

// ...

    @Override
    protected void onResume() {
        super.onResume();
        // Garante que o Presenter está ouvindo os callbacks do serviço
        // quando a activity está em primeiro plano.
        if (presenter != null) {
            presenter.aoRetomar();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Limpa o callback quando a activity não está mais em primeiro plano
        // para evitar vazamentos de memória (memory leaks).
        if (presenter != null) {
            presenter.aoPausar();
        }
    }


    @Override
    public void mensagemErro(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void pedirPermissaoGravacaoAudio() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
    }
    //endregion

    //region Interação com Adapter e UI


    @Override
    public void onAddToPlaylistClicked(Song song) {
        Intent intent = new Intent(this, BibliotecaActivity.class);
        startActivity(intent);
    }

    private void configurarClickListeners() {
        btnPlayPause.setOnClickListener(v -> presenter.tocarOuPausar());
        findViewById(R.id.btn_next).setOnClickListener(v -> presenter.proximaMusica());
        findViewById(R.id.btn_previous).setOnClickListener(v -> presenter.musicaAnterior());

    }

    private void configurarSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { if (u) textCurrentTime.setText(formatarTempo(p)); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) { presenter.avancarPara(s.getProgress()); }
        });
    }

    private String formatarTempo(long ms) {
        long min = (ms / 1000) / 60;
        long sec = (ms / 1000) % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", min, sec);
    }
    //endregion

    //region Menu, Toolbar e Navegação
    private void configurarToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }
// Em MainActivity.java

    @Override
    protected void onStart() {
        super.onStart();
        // Assim que a Activity fica visível, informamos o Presenter,
        // que por sua vez comanda o serviço para entrar em modo foreground com segurança.
        if (presenter != null) {
            presenter.onActivityIsVisible();
        }
    }
// Em presenter/Presenter.java

    // Adicione este novo método à sua classe Presenter


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            public boolean onQueryTextSubmit(String q) { return false; }
            public boolean onQueryTextChange(String n) { songAdapter.getFilter().filter(n); return true; }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_biblioteca) {
            startActivityForResult(new Intent(this, BibliotecaActivity.class), REQUEST_CODE_BIBLIOTECA);
            return true;
        } else if (id == R.id.action_playlists) {
            startActivityForResult(new Intent(this, PlaylistsActivity.class), REQUEST_CODE_PLAYLISTS);
            return true;
        } else if (id == R.id.action_recognize) {
            presenter.iniciarReconhecimentoMusica();
            return true;
        }  else if (id == R.id.action_music_details) {
            // Pede ao Presenter para mostrar os detalhes da música atual
            if (presenter != null) {
                presenter.mostrarDetalhesDaMusicaAtual();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && presenter != null) {
            if (requestCode == REQUEST_CODE_BIBLIOTECA) {
                int pos = data.getIntExtra("POSICAO_MUSICA_TOCAR", -1);
                if (pos != -1) presenter.tocarTodasAsMusicasNaPosicao(pos);
            } else if (requestCode == REQUEST_CODE_PLAYLISTS) {
                if ("TOCAR_PLAYLIST".equals(data.getAction())) {
                    long pId = data.getLongExtra("PLAYLIST_ID", -1);
                    if (pId != -1) presenter.tocarPlaylistNaPosicao(pId, 0);
                }
            }
        }
    }

    private void checkMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            iniciarReconhecimentoACR();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarReconhecimentoACR();
            } else {
                Toast.makeText(this, "Permissão de microfone negada", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void iniciarReconhecimentoACR() {
        if (acrCloudClient == null) {
            Toast.makeText(this, "ACRCloud não inicializado", Toast.LENGTH_SHORT).show();
            return;
        }

        // Cancela qualquer sessão anterior
        acrCloudClient.cancel();

        // Verifica permissão antes de tudo
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Inicia o reconhecimento
        boolean success = acrCloudClient.startRecognize();

        if (success) {
            Toast.makeText(this, "🎧 Ouvindo... Fale ou toque uma música próxima ao microfone", Toast.LENGTH_LONG).show();
            Log.d("ACRCloud", "Reconhecimento iniciado com sucesso.");
        } else {
            Toast.makeText(this, " Falha ao iniciar reconhecimento", Toast.LENGTH_SHORT).show();
            Log.e("ACRCloud", "Erro ao iniciar reconhecimento.");
        }
    }


    @Override
    public void exibirDetalhesDaMusica(Song musica) {
        // Se o objeto 'musica' for nulo, significa que nada está tocando.
        if (musica == null) {
            mensagemInformativa("Nenhuma música está tocando no momento.");
            return;
        }

        // Usa um StringBuilder para construir a mensagem formatada.
        StringBuilder detalhes = new StringBuilder();
        detalhes.append("Título: ").append(musica.title).append("\n\n");
        detalhes.append("Artista: ").append(musica.artist).append("\n\n");
        // Verifica se o álbum não é nulo ou vazio antes de exibir.
        detalhes.append("Álbum: ").append(musica.album != null ? musica.album : "Desconhecido").append("\n\n");
        detalhes.append("Duração: ").append(formatarTempo(musica.duration)).append("\n\n");
        // Verifica se o caminho não é nulo antes de exibir.
        detalhes.append("Caminho: ").append(musica.path != null ? musica.path : "N/A");

        // Garante que o diálogo seja exibido na thread principal da UI.
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle("Detalhes da Música")
                    .setMessage(detalhes.toString())
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

// metodo para adicionar as credenciais
    @SuppressLint("UnsafeOptInUsageError")
    private void initializeACRCloud() {
        ACRCloudConfig config = new ACRCloudConfig();
        config.acrcloudListener = this;
        config.context = this;
        config.host = "identify-eu-west-1.acrcloud.com";
        config.accessKey = "a7add414387ce255924fa2ce8add0393";
        config.accessSecret = "jZQVusg0qbthYGDwwwTzScAfkemKQaj4yhAFOemw";

        // Configuração do gravador — versão compatível
        config.recorderConfig.rate = 44100;
        config.recorderConfig.channels = 1;

        acrCloudClient = new ACRCloudClient();
        boolean initialized = acrCloudClient.initWithConfig(config);

        if (!initialized) {
            Toast.makeText(this, "Falha ao inicializar ACRCloud", Toast.LENGTH_SHORT).show();
            Log.e("ACRCloud", "Falha na inicialização do ACRCloud.");
        } else {
            Log.d("ACRCloud", "ACRCloud inicializado com sucesso.");
        }
    }

    private void startRecognition() {
        if (acrCloudClient != null && !acrCloudClient.startRecognize()) {
            mensagemErro("Erro ao iniciar reconhecimento.");
        } else {
            mensagemInformativa("Ouvindo...");
        }
    }
    private void showRecognitionResultDialog(String title, String message) {
        runOnUiThread(() -> {
            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .show();
        });
    }

    @Override
    public void onResult(ACRCloudResult result) {
        if (acrCloudClient != null) {
            acrCloudClient.cancel();
        }

        if (result == null) {
            runOnUiThread(() ->
                    showRecognitionResultDialog("Erro", "Nenhum resultado retornado do ACRCloud."));
            return;
        }

        String resultText = result.getResult();
        Log.d("ACRCloud", "Resultado bruto: " + resultText);

        if (resultText == null || resultText.trim().isEmpty()) {
            runOnUiThread(() ->
                    showRecognitionResultDialog("Erro", "A resposta do ACRCloud veio vazia."));
            return;
        }

        try {
            JSONObject json = new JSONObject(resultText);

            if (!json.has("status")) {
                runOnUiThread(() ->
                        showRecognitionResultDialog("Erro", "Resposta inesperada do servidor ACRCloud."));
                return;
            }

            JSONObject status = json.getJSONObject("status");
            String msg = status.optString("msg", "Desconhecido");
            int code = status.optInt("code", -1);

            String dialogTitle;
            String dialogMessage;

            if (code == 0) {
                JSONObject metadata = json.optJSONObject("metadata");
                if (metadata != null && metadata.has("music")) {
                    JSONObject music = metadata.getJSONArray("music").getJSONObject(0);
                    String songTitle = music.optString("title", "Desconhecido");
                    String artistName = music.getJSONArray("artists")
                            .getJSONObject(0)
                            .optString("name", "Artista Desconhecido");

                    dialogTitle = "🎶 Música Reconhecida";
                    dialogMessage = "Título: " + songTitle + "\nArtista: " + artistName;
                } else {
                    dialogTitle = "Resultado";
                    dialogMessage = "Nenhuma informação musical encontrada.";
                }

            } else if (code == 1001) {
                dialogTitle = "Sem Resultado";
                dialogMessage = "Nenhuma música correspondente foi encontrada na base de dados.";

            } else {
                dialogTitle = "Erro de Reconhecimento";
                dialogMessage = "Não foi possível identificar a música.\n\nMotivo: "
                        + msg + "\nCódigo do erro: " + code;
            }

            runOnUiThread(() -> showRecognitionResultDialog(dialogTitle, dialogMessage));

        } catch (JSONException e) {
            Log.e("ACRCloud", "Erro ao processar JSON: " + e.getMessage());
            runOnUiThread(() ->
                    showRecognitionResultDialog("Erro Crítico", "Falha ao processar a resposta da API."));
        }
    }



    @Override
    public void onVolumeChanged(double volume) {
        Log.d("ACRCloud", "Volume: " + volume);
    }
    @Override
    public void mostrarResultadoReconhecimento(String title, String artist, String album) {
        runOnUiThread(() -> new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage("Artista: " + artist)
                .setPositiveButton("OK", null).show());
    }

    @Override
    public void mensagemInformativa(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    //endregion

    //region Ciclo de Vida e Permissões


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (presenter != null) presenter.aoDestruir();
        if (acrCloudClient != null) acrCloudClient.release();
    }
    //endregion
}
