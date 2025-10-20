// Local: presenter/Presenter.java
package com.example.musicplayera.presenter;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.media3.common.util.Log; // Mantive seu import de Log

import com.example.musicplayera.Model.Song;
import com.example.musicplayera.Repositorio.MusicRepository;
import com.example.musicplayera.Servico.MusicService;
import com.example.musicplayera.view.ContratoView;

import java.util.ArrayList;
import java.util.List;
// o cerebro do projeto
public class Presenter implements MusicService.MusicServiceCallback {

    private ContratoView view;
    private final Context context;
    private final MusicRepository repository;
    private MusicService musicService;
    private boolean isServiceBound = false;
    private List<Song> listaDeMusicasAtual = new ArrayList<>();


    private boolean toquePendente = false;
    private int posicaoPendente = -1;

   // Servico para o backbround service
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            musicService.setCallback(Presenter.this);

            Log.d("Presenter", "MusicService conectado e pronto.");

            sincronizarEstadoDaUI();

            if (toquePendente) {
                toquePendente = false;
                tocarTodasAsMusicasNaPosicao(posicaoPendente);
            }
        }


        @SuppressLint("UnsafeOptInUsageError")
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
            Log.d("Presenter", "MusicService desconectado.");
        }
    };

    // contrutor
    public Presenter(ContratoView view, Context context) {
        this.view = view;
        this.context = context.getApplicationContext();
        this.repository = new MusicRepository(this.context);
        ligarAoServico();
    }
// intent que liga o servico
    private void ligarAoServico() {
        Intent intent = new Intent(context, MusicService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    //metodo que carrega as musicas do dispositico
    @SuppressLint("UnsafeOptInUsageError")
    public void carregarMusicasIniciais() {
        // Primeiro, verifica se o serviço já tem uma playlist ativa
        if (isServiceBound && musicService != null && musicService.hasActivePlaylist()) {
            Log.d("Presenter", "Playlist já ativa no serviço. Não carregando novamente.");

            return;
        }

        // escaneia e carrega do banco
        repository.escanearEsalvarMusicasDoDispositivo(new MusicRepository.RepositoryCallback<Void>() {
            @Override
            public void onComplete(Void result) {
                repository.obterTodasAsMusicas(new MusicRepository.RepositoryCallback<List<Song>>() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void onComplete(List<Song> songs) {
                        listaDeMusicasAtual = songs;
                        if (view != null) {
                            view.atualizarListaMusicas(songs);
                        }
                        // Só define a playlist no serviço se ele NÃO tiver uma
                        if (isServiceBound && musicService != null && !songs.isEmpty() && !musicService.hasActivePlaylist()) {
                            Log.d("Presenter", "Definindo playlist inicial no MusicService com " + songs.size() + " músicas.");
                            musicService.setPlaylist(songs);
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        if (view != null) view.mensagemErro("Erro ao carregar músicas do banco de dados.");
                    }
                });
            }
            @Override
            public void onError(Exception e) {
                if (view != null) view.mensagemErro("Erro ao escanear dispositivo: " + e.getMessage());
            }
        });
    }

    private void sincronizarEstadoDaUI() {
        if (isServiceBound && musicService != null && view != null) {
            // 1. Atualiza shuffle/repeat
            boolean isShuffleEnabled = musicService.isShuffleModeEnabled();
            int repeatMode = musicService.getRepeatMode();
            view.modoEmbaralharMudou(isShuffleEnabled);
            view.modoRepetirMudou(repeatMode);

            // 2. Atualiza a música atual
            Song currentSong = musicService.getCurrentSong();
            if (currentSong != null) {
                view.musicaMudou(currentSong);
            }

            // 3. Atualiza o estado de play/pause
            boolean isPlaying = musicService.isPlaying();
            view.estadoReproducaoMudou(isPlaying);

            // 4. Atualiza a lista de músicas com a playlist REAL do serviço
            List<Song> playlistAtiva = musicService.getCurrentPlaylist();
            if (playlistAtiva != null && !playlistAtiva.isEmpty()) {
                listaDeMusicasAtual = new ArrayList<>(playlistAtiva); // cópia segura
                view.atualizarListaMusicas(playlistAtiva);
            }

            // 5. ⭐️ Força a atualização do progresso (posição e duração)
            long pos = musicService.getCurrentPosition();
            long dur = musicService.getDuration();
            if (dur > 0) {
                view.atualizarProgresso(pos, dur);
            }


        }
    }

    // Métodos de toque
    @SuppressLint("UnsafeOptInUsageError")
    public void tocarTodasAsMusicasNaPosicao(int posicao) {
        if (isServiceBound && musicService != null) {
            // ========== LÓGICA ATUALIZADA ==========
            // Não chamamos mais setPlaylist aqui. Apenas mandamos tocar.
            // O MusicService já tem a playlist.
            Log.d("Presenter", "Comando para o serviço tocar na posição: " + posicao);
            musicService.playSongAtIndex(posicao);
            // =======================================
        } else {
            Log.w("Presenter", "Serviço não conectado. Agendando toque na posição " + posicao + " para mais tarde.");
            // A lógica de toque pendente continua a mesma.
            toquePendente = true;
            posicaoPendente = posicao;
        }
    }

// metodo para ver detalhes de musicas
    public void mostrarDetalhesDaMusicaAtual() {
        // Verifica se o serviço existe e se há algo tocando ou pausado.
        if (musicService != null && musicService.estaTocandoOuPausado()) {
            // Pede ao serviço a música que está ativa.
            Song musicaAtual = musicService.getMusicaAtual(); // <-- Usará o método corrigido

            // Envia a música (ou null se não houver) para a MainActivity (view).
            if (view != null) {
                view.exibirDetalhesDaMusica(musicaAtual);
            }
        } else {
            // Se nada estiver tocando, informa à view que não há música.
            if (view != null) {
                view.exibirDetalhesDaMusica(null);
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void tocarMusicaAgora(int posicao) {
        if (listaDeMusicasAtual != null && !listaDeMusicasAtual.isEmpty()) {
            Log.d("Presenter", "Comando REAL para tocar música na posição: " + posicao);
            musicService.setPlaylist(listaDeMusicasAtual); // <<<<<< A lista é enviada AQUI
            musicService.playSongAtIndex(posicao);
        } else {
            Log.e("Presenter", "Não foi possível tocar: lista de músicas vazia.");
        }
    }

    // O resto da sua classe (tocarPlaylist, onActivityIsVisible, tocarOuPausar, etc.) permanece o mesmo.
    // Colei abaixo para garantir que nada seja perdido.

    public void tocarPlaylistNaPosicao(long playlistId, int position) {
        repository.obterMusicasDaPlaylist(playlistId, new MusicRepository.RepositoryCallback<List<Song>>() {
            @Override
            public void onComplete(List<Song> songs) {
                if (isServiceBound && songs != null && !songs.isEmpty()) {
                    listaDeMusicasAtual = songs;
                    musicService.setPlaylist(songs);
                    musicService.playSongAtIndex(position);
                    if (view != null) {
                        view.atualizarListaMusicas(songs);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                if (view != null) view.mensagemErro("Erro ao carregar playlist.");
            }
        });
    }

    public void onActivityIsVisible() {
        if (isServiceBound) {

        }
    }

    public boolean estaTocando() {
        if (isServiceBound && musicService != null) {
            return musicService.isPlaying();
        }
        return false;
    }

    /**
     * Este método deve ser chamado no onResume() da Activity.
     */
    @SuppressLint("UnsafeOptInUsageError")
    public void aoRetomar() {
        if (isServiceBound && musicService != null) {
            musicService.setCallback(this);
            Log.d("Presenter", "Callback do MusicService redefinido em aoRetomar.");

            sincronizarEstadoDaUI();

        }
    }


    @SuppressLint("UnsafeOptInUsageError")
    public void aoPausar() {
        if (isServiceBound && musicService != null) {
            musicService.setCallback(null);
            Log.d("Presenter", "Callback do MusicService limpo em aoPausar.");
        }
    }

    public void tocarOuPausar() { if (isServiceBound) musicService.playOrPause(); }
    public void proximaMusica() { if (isServiceBound) musicService.nextSong(); }
    public void musicaAnterior() { if (isServiceBound) musicService.prevSong(); }
    public void avancarPara(long position) {
        if (isServiceBound) {
            musicService.seekTo(position); // position já é long
        }
    }
// Em presenter/Presenter.java

// ... (após os métodos proximaMusica, musicaAnterior, etc.)

    @SuppressLint("UnsafeOptInUsageError")
    public void alternarModoRepeticao() {
        if (isServiceBound && musicService != null && view != null) {
            // 1. Chama o método no serviço
            int novoModo = musicService.toggleRepeatMode();

            // 2. USA O RETORNO IMEDIATO para atualizar a view
            Log.d("Presenter", "Repeat alternado. Novo modo IMEDIATO: " + novoModo);
            view.modoRepetirMudou(novoModo);
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void alternarModoShuffle() {
        if (isServiceBound && musicService != null && view != null) {
            boolean novoEstado = musicService.toggleShuffleMode();

            // 2. USA O RETORNO IMEDIATO para atualizar a view
            Log.d("Presenter", "Shuffle alternado. Novo estado IMEDIATO: " + novoEstado);
            view.modoEmbaralharMudou(novoEstado);
        }
    }

    public void iniciarReconhecimentoMusica() {
        if (view != null) view.iniciarReconhecimentoACR();
    }

    public void aoDestruir() {
        if (isServiceBound) {
            context.unbindService(serviceConnection);
            isServiceBound = false;
        }
        view = null;
    }

    @Override public void onMusicChanged(Song newSong) { if (view != null) view.musicaMudou(newSong); }
    @Override public void onProgressUpdate(long pos, long dur) { if (view != null) view.atualizarProgresso(pos, dur); }
    @Override public void onPlaybackStateChanged(boolean isPlaying) { if (view != null) view.estadoReproducaoMudou(isPlaying); }
    @Override public void onShuffleModeChanged(boolean isEnabled) { if (view != null) view.modoEmbaralharMudou(isEnabled); }
    @Override public void onRepeatModeChanged(int repeatMode) { if (view != null) view.modoRepetirMudou(repeatMode); }
}
