package com.projectdecuple;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectdecuple.Core.Music.AudioInfo;
import com.projectdecuple.Core.Music.GuildMusicManager;
import com.projectdecuple.Core.Music.TrackScheduler;
import com.projectdecuple.Core.Utility.EasyEqual;
import com.projectdecuple.Core.Utility.ReadFile;
import com.projectdecuple.Core.YoutubeAPI;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.*;

public class MusicListener extends ListenerAdapter {

    private static final JsonParser jsonParser = new JsonParser();
    private static final JsonObject message = (JsonObject) jsonParser.parse(Objects.requireNonNull(new ReadFile().readString(BotBuilder.BASE_DIRECTORY + "/Config/MESSAGE.json")));
    private static final JsonObject command = (JsonObject) jsonParser.parse(Objects.requireNonNull(new ReadFile().readString(BotBuilder.BASE_DIRECTORY + "/Config/COMMAND.json")));

    public String CONNECT_VOICE_CHANNEL = message.get("connectVoiceChannel").getAsString();
    public String DISCONNECT_VOICE_CHANNEL = message.get("disconnectVoiceChannel").getAsString();
    public String CANNOT_CONNECT_VOICE_CHANNEL = message.get("cannotConnectVoiceChannel").getAsString();
    public String ADDED_MUSIC_IN_QUEUE = message.get("addedMusicInQueue").getAsString();
    public String CANNOT_FIND_SEARCH_RESULTS_IN_YOUTUBE = message.get("cannotFindSearchResultsInYoutube").getAsString();
    public String CANNOT_LOAD_TRACK = message.get("cannotLoadTrack").getAsString();
    public String SKIP_TRACK = message.get("skipTrack").getAsString();
    public String SKIP_TRACKS = message.get("skipTracks").getAsString();
    public String SKIP_ALL_TRACKS = message.get("skipAllTracks").getAsString();
    public String OUT_OF_SKIP_TRACKS_RANGE = message.get("outOfSkipTracksRange").getAsString();
    public String SET_LOWER_VOLUME = message.get("setLowerVolume").getAsString();
    public String SET_HIGHER_VOLUME = message.get("setHigherVolume").getAsString();
    public String NOW_PLAYING_TRACK_INFO = message.get("nowPlayingTrackInfo").getAsString();
    public String NOT_PLAYING_TRACK_MESSAGE = message.get("notPlayingTrackMessage").getAsString();
    public String OUT_OF_CHART_RANGE = message.get("outOfChartRange").getAsString();
    public String SHUFFLED_QUEUE = message.get("shuffledQueue").getAsString();
    public String ENABLED_REPEAT_TRACK = message.get("enabledRepeatTrack").getAsString();
    public String DISABLED_REPEAT_TRACK = message.get("disabledRepeatTrack").getAsString();
    public String QUEUE_EMPTY_MESSAGE = message.get("queueEmptyMessage").getAsString();
    public String NOW_PLAYING_QUEUE_LIST = message.get("nowPlayingQueue").getAsString();
    public String SHUTDOWN_BOT = message.get("shutdownBot").getAsString();
    public String TIME_STAMP = message.get("timeStamp").getAsString();

    public String CONNECT_COMMAND = command.get("connectCommand").getAsString();
    public String DISCONNECT_COMMAND = command.get("disconnectCommand").getAsString();
    public String QUEUE_COMMAND = command.get("queueCommand").getAsString();
    public String SKIP_COMMAND = command.get("skipCommand").getAsString();
    public String VOLUME_COMMAND = command.get("volumeCommand").getAsString();
    public String NOW_PLAYING_COMMAND = command.get("nowPlayingCommand").getAsString();
    public String SHUFFLE_COMMAND = command.get("shuffleCommand").getAsString();
    public String REPEAT_COMMAND = command.get("repeatCommand").getAsString();
    public String SHOW_LIST_COMMAND = command.get("showListCommand").getAsString();
    public String SHUTDOWN_COMMAND = command.get("shutdownCommand").getAsString();

    public char PREFIX = command.get("prefix").getAsString().charAt(0);

    public static final EasyEqual e = new EasyEqual();

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    StringBuffer sb = new StringBuffer();
    Formatter f = new Formatter(sb);

    public MusicListener() {
        this.musicManagers = new HashMap();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        sb = new StringBuffer();
        f = new Formatter(sb);

        User user = event.getAuthor();
        Member member = event.getMember();
        TextChannel c = event.getChannel();
        Message msg = event.getMessage();
        Guild gld = event.getGuild();

        AudioManager am = gld.getAudioManager();

        if (user.isBot()) return;

        String[] args = msg.getContentRaw().substring(1).split(" ");
        char firstCharacter = msg.getContentRaw().charAt(0);

        if (firstCharacter == PREFIX) {

            if (member == null) return;
            if (args.length <= 0) return;

            if (e.eq(args[0], CONNECT_COMMAND)) {

                try {

                    VoiceChannel v = Objects.requireNonNull(member.getVoiceState()).getChannel();

                    if (v != null) {
                        am.setSendingHandler(new AudioSendHandler() {
                            @Override
                            public boolean canProvide() {
                                return false;
                            }

                            @Nullable
                            @Override
                            public ByteBuffer provide20MsAudio() {
                                return null;
                            }
                        });

                        am.openAudioConnection(v);
                        // TODO : Set Volume

                        f.format(CONNECT_VOICE_CHANNEL, v.getName());
                        c.sendMessage(f.toString()).queue();
                    }

                } catch (NullPointerException e) {
                    c.sendMessage(CANNOT_CONNECT_VOICE_CHANNEL).queue();
                }

            }

            if (e.eq(args[0], DISCONNECT_COMMAND)) {

                skipAllTrack(c, false, gld);
                am.closeAudioConnection();
                c.sendMessage(DISCONNECT_VOICE_CHANNEL).queue();

            }

            if (e.eq(args[0], QUEUE_COMMAND)) {

                String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                try {

                    VoiceChannel v = Objects.requireNonNull(member.getVoiceState()).getChannel();

                    if (v != null) {
                        setVolume(c, 20, false);

                        am.setSendingHandler(new AudioSendHandler() {
                            @Override
                            public boolean canProvide() {
                                return false;
                            }

                            @javax.annotation.Nullable
                            @Override
                            public ByteBuffer provide20MsAudio() {
                                return null;
                            }
                        });

                        am.openAudioConnection(v);

                        f.format(CONNECT_VOICE_CHANNEL, v.getName());
                        c.sendMessage(f.toString()).queue();
                    }

                    YoutubeAPI youtube = new YoutubeAPI();

                    String youtubeSearched = youtube.searchYoutube(input);

                    if (youtubeSearched == null) {
                        c.sendMessage(CANNOT_FIND_SEARCH_RESULTS_IN_YOUTUBE).queue();
                        return;
                    }

                    input = youtubeSearched;
                    loadAndPlay(event.getChannel(), input, true, member);


                } catch (NullPointerException ex) {

                    c.sendMessage(CANNOT_CONNECT_VOICE_CHANNEL).queue();

                }

            }

            if (e.eq(args[0], SKIP_COMMAND)) {

                if (args.length == 1) {
                    skipTrack(c, true);
                } else if (e.eq(args[1], "ALL", "모두", "전체", "전부")) {
                    skipAllTrack(c, true, gld);
                } else {
                    int skipTrack = Integer.parseInt(args[1]);

                    if (skipTrack < 1 | skipTrack > 10) {
                        c.sendMessage(OUT_OF_SKIP_TRACKS_RANGE).queue();
                    } else {
                        skipTrack(c, Integer.parseInt(args[1]), true);
                    }
                }

            }

            if (e.eq(args[0], VOLUME_COMMAND)) {

                if (args.length == 1) return;

                int vol = Integer.parseInt(args[1]);
                setVolume(c, vol, true);

            }

            if (e.eq(args[0], NOW_PLAYING_COMMAND)) {
                sendNowPlaying(c);
            }

            if (e.eq(args[0], SHUFFLE_COMMAND)) {
                GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                musicManager.scheduler.shuffle();

                c.sendMessage(SHUFFLED_QUEUE).queue();
            }

            if (e.eq(args[0], REPEAT_COMMAND)) {
                GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                musicManager.scheduler.setRepeating(true);

                c.sendMessage(musicManager.scheduler.isRepeating() ? ENABLED_REPEAT_TRACK : DISABLED_REPEAT_TRACK).queue();
            }

            if (e.eq(args[0], SHOW_LIST_COMMAND)) {
                GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                TrackScheduler scheduler = musicManager.scheduler;

                Queue<AudioInfo> queue = scheduler.queue;

                if (queue.isEmpty()) {

                    c.sendMessage(QUEUE_EMPTY_MESSAGE).queue();

                } else {

                    int trackCount = 0;
                    long queueLength = 0;
                    StringBuilder sb = new StringBuilder();

                    f.format(NOW_PLAYING_QUEUE_LIST, queue.size());

                    sb.append("```md\n# ").append(f.toString()).append("\n\n");

                    for (AudioInfo info : queue) {
                        queueLength += info.getTrack().getDuration();

                        if (trackCount < 20) {
                            sb.append(trackCount + 1).append(". ").append(info.getTrack().getInfo().title).append("\n");
                            trackCount++;
                        }
                    }

                    sb.append("\n전체 재생 길이 : ").append(getTimeStamp(queueLength, true)).append("```");
                    c.sendMessage(sb.toString()).queue();

                }
            }

            if (e.eq(args[0], SHUTDOWN_COMMAND)) {
                c.sendMessage(SHUTDOWN_BOT).queue();
                System.exit(-1);
            }

        }

    }

    public void loadAndPlay(final TextChannel tc, String url, boolean showMessage, Member user) {

        sb = new StringBuffer();
        f = new Formatter(sb);
        if (user == null) return;

        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());

        final String trackUrl;


        if (url.startsWith("<") && url.endsWith(">"))
            trackUrl = url.substring(1, url.length() - 1);
        else
            trackUrl = url;

        playerManager.loadItemOrdered(musicManager, trackUrl, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {

                long af = audioTrack.getInfo().length;

                int seconds = (int) (af / 1000) % 60;
                int minutes = (int) ((af / (1000 * 60)) % 60);
                int hours = (int) ((af / (1000 * 60 * 60)));

                String timeStamp = f.format(TIME_STAMP, hours, minutes, seconds).toString();

                sb = new StringBuffer();
                f = new Formatter(sb);

                EmbedBuilder eb = new EmbedBuilder();
                f.format(ADDED_MUSIC_IN_QUEUE, audioTrack.getInfo().title, audioTrack.getInfo().author, timeStamp);

                eb.setDescription("[" + f.toString() + "](" + trackUrl + ")]");
                eb.setColor(Color.CYAN);

                YoutubeAPI y = new YoutubeAPI();
                eb.setImage(y.getThumbnail(trackUrl));

                eb.setFooter(user.getUser().getAsTag(), user.getUser().getAvatarUrl());

                if (showMessage) {
                    tc.sendMessage(eb.build()).queue();
                }

                play(tc.getGuild(), musicManager, audioTrack, user, tc);

            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                AudioTrack firstTrack = audioPlaylist.getSelectedTrack();

                long af = firstTrack.getInfo().length;

                int seconds = (int) (af / 1000) % 60;
                int minutes = (int) ((af / (1000 * 60)) % 60);
                int hours = (int) ((af / (1000 * 60 * 60)));

                String timeStamp = f.format(TIME_STAMP, hours, minutes, seconds).toString();

                sb = new StringBuffer();
                f = new Formatter(sb);

                EmbedBuilder eb = new EmbedBuilder();
                f.format(ADDED_MUSIC_IN_QUEUE, firstTrack.getInfo().title, firstTrack.getInfo().author, timeStamp);

                eb.setDescription("[" + f.toString() + "](" + trackUrl + ")]");
                eb.setColor(Color.CYAN);

                YoutubeAPI y = new YoutubeAPI();
                eb.setImage(y.getThumbnail(trackUrl));

                eb.setFooter(user.getUser().getAsTag(), user.getUser().getAvatarUrl());

                if (showMessage) {
                    tc.sendMessage(eb.build()).queue();
                }

                play(tc.getGuild(), musicManager, firstTrack, user, tc);
            }

            @Override
            public void noMatches() {
                EmbedBuilder eb = new EmbedBuilder();

                if (showMessage) {

                    eb.setDescription(CANNOT_FIND_SEARCH_RESULTS_IN_YOUTUBE);
                    eb.setColor(Color.RED);

                    tc.sendMessage(eb.build()).queue();

                }
            }

            @Override
            public void loadFailed(FriendlyException e) {
                EmbedBuilder eb = new EmbedBuilder();

                if (showMessage) {
                    eb.setDescription(CANNOT_LOAD_TRACK);
                    eb.setColor(Color.RED);

                    tc.sendMessage(eb.build()).queue();
                }
            }
        });

    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {

        sb = new StringBuffer();
        f = new Formatter(sb);
        long guildId = Long.parseLong(guild.getId());
        GuildMusicManager musicManager = musicManagers.get(guildId);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guildId, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());
        return musicManager;

    }

    public void play(Guild guild, GuildMusicManager musicManager, AudioTrack track, Member member, TextChannel tc) {
        connectToFirstVoiceChannel(guild.getAudioManager());
        musicManager.scheduler.queue(track, member, tc);
    }

    public void skipTrack(TextChannel tc, boolean showMessage) {

        sb = new StringBuffer();
        f = new Formatter(sb);
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
        musicManager.scheduler.nextTrack();

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setDescription(SKIP_TRACK);
            eb.setColor(Color.CYAN);

            tc.sendMessage(eb.build()).queue();
        }

    }

    public void skipTrack(TextChannel tc, int value, boolean showMessage) {

        sb = new StringBuffer();
        f = new Formatter(sb);
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
        musicManager.scheduler.nextTrack(value);

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            f.format(SKIP_TRACKS, value);

            eb.setDescription(f.toString());
            eb.setColor(Color.CYAN);

            tc.sendMessage(eb.build()).queue();
        }

    }

    public void skipAllTrack(TextChannel tc, boolean showMessage, Guild guild) {
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());

        sb = new StringBuffer();
        f = new Formatter(sb);

        musicManager.pl.stopTrack();
        musicManager.scheduler.queue.clear();
        musicManager.pl.destroy();

        musicManagers.remove(guild.getIdLong());
        guild.getAudioManager().setSendingHandler(null);

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setDescription(SKIP_ALL_TRACKS);

            tc.sendMessage(eb.build()).queue();
        }
    }

    public void setVolume(TextChannel tc, int volume, boolean showMessage) {
        sb = new StringBuffer();
        f = new Formatter(sb);

        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());

        int prVol = musicManager.scheduler.pl.getVolume();
        musicManager.scheduler.pl.setVolume(volume);

        EmbedBuilder eb = new EmbedBuilder();

        if (prVol > volume) {
            eb.setColor(Color.ORANGE);
            eb.setTitle(SET_LOWER_VOLUME);
        } else if (prVol == volume) {
            eb.setColor(Color.YELLOW);
            showMessage = false;
        } else {
            eb.setColor(Color.CYAN);
            eb.setTitle(SET_HIGHER_VOLUME);
        }
        eb.setDescription(prVol + " :arrow_forward: " + volume);

        if (!showMessage) return;
        tc.sendMessage(eb.build()).queue();
    }

    public void sendNowPlaying(TextChannel tc) {
        sb = new StringBuffer();
        f = new Formatter(sb);

        try {
            GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
            AudioTrack audioTrack = musicManager.scheduler.pl.getPlayingTrack();

            AudioTrackInfo af = audioTrack.getInfo();
            EmbedBuilder eb = new EmbedBuilder();

            long as = audioTrack.getInfo().length;

            int seconds = (int) (as / 1000) % 60;
            int minutes = (int) ((as / (1000 * 60)) % 60);
            int hours = (int) ((as / (1000 * 60 * 60)));

            String timeStamp = f.format(TIME_STAMP, hours, minutes, seconds).toString();
            f.format(NOW_PLAYING_TRACK_INFO, audioTrack.getInfo().title, audioTrack.getInfo().author, timeStamp);

            eb.setDescription("[" + f.toString() + "](" + af.uri + ")]");

            tc.sendMessage(eb.build()).queue();
        } catch (NullPointerException e) {
            tc.sendMessage(NOT_PLAYING_TRACK_MESSAGE).queue();
        }
    }

    public String getTimeStamp(long pos, boolean korean) {

        int ns = (int) (pos / 1000) % 60;
        int nm = (int) (pos / (1000 * 60)) % 60;
        int nh = (int) (pos / (1000 * 60 * 60));

        if (korean) {
            return nh + "시간 " + nm + "분 " + ns + "초";
        } else {
            return nh + ":" + nm + ":" + ns + ":";
        }

    }

    public static void connectToFirstVoiceChannel(AudioManager audioManager) {
        if (!audioManager.isConnected()) { // 'audioManager.isAttemptingConnect()' was deprecated
            for (VoiceChannel vc : audioManager.getGuild().getVoiceChannels()) {
                audioManager.openAudioConnection(vc);
                break;
            }
        }
    }

}

