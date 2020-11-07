package com.projectdecuple;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectdecuple.Core.DecupleAPI;
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
import java.io.File;
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
    // public String OUT_OF_CHART_RANGE = message.get("outOfChartRange").getAsString();
    public String SHUFFLED_QUEUE = message.get("shuffledQueue").getAsString();
    public String ENABLED_REPEAT_TRACK = message.get("enabledRepeatTrack").getAsString();
    public String DISABLED_REPEAT_TRACK = message.get("disabledRepeatTrack").getAsString();
    public String QUEUE_EMPTY_MESSAGE = message.get("queueEmptyMessage").getAsString();
    public String NOW_PLAYING_QUEUE_LIST = message.get("nowPlayingQueue").getAsString();
    public String SHUTDOWN_BOT = message.get("shutdownBot").getAsString();
    public String TIME_STAMP = message.get("timeStamp").getAsString();
    public String PLAY_PLAYLISTS = message.get("playPlaylist").getAsString();
    public String SEND_PLAYLISTS_INFORMATION = message.get("sendPlaylistInfo").getAsString();

    /* Before v1.2.0
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
     */

    public enum Command {

        CONNECT_COMMAND(cmdGetter("connectCommand")),
        DISCONNECT_COMMAND(cmdGetter("disconnectCommand")),
        QUEUE_COMMAND(cmdGetter("queueCommand")),
        SKIP_COMMAND(cmdGetter("skipCommand")),
        VOLUME_COMMAND(cmdGetter("volumeCommand")),
        NOW_PLAYING_COMMAND(cmdGetter("nowPlayingCommand")),
        SHUFFLE_COMMAND(cmdGetter("shuffleCommand")),
        REPEAT_COMMAND(cmdGetter("repeatCommand")),
        SHOW_LIST_COMMAND(cmdGetter("showListCommand")),
        SHUTDOWN_COMMAND(cmdGetter("shutdownCommand")),
        PLAYLISTS_COMMAND(cmdGetter("customPlaylistCommand")),
        SHOW_PLAYLISTS_COMMAND(cmdGetter("listGetterCommand"));

        private final String[] command;

        Command(String[] command) {
            this.command = command;
        }

        String[] getCommand() {
            return command;
        }

    }

    public char PREFIX = command.get("prefix").getAsString().charAt(0);

    public static final EasyEqual e = new EasyEqual();

    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    StringBuffer sb = new StringBuffer();

    // -- Using Utils -- //
    public static String[] jsonArrayToStrArray(JsonArray arrays) {

        ArrayList<String> result = new ArrayList<>();

        for (JsonElement strArray : arrays) {
            result.add(strArray.getAsString());
        }

        return result.toArray(new String[arrays.size()]);

    }

    public static String[] cmdGetter(String get) {

        return command.get(get).getClass() == JsonArray.class ?
                jsonArrayToStrArray(command.get(get).getAsJsonArray()) :
                new String[] {command.get(get).getAsJsonPrimitive().getAsString()};

    }

    public String r(String str, String ... rp) {

        // Array replacer description
        // How to Use : `r(String, "FromString", "ToString", "FromString2", "ToString2"...)`;
        // Example : `r("Hello, Mr. My Yesterday", "Hello,", "Hi!", " Mr.", "", "Yesterday", "Name is 'CustomMusicBot'!")`

        // Example BF/AF
        // Before : "Hello, Mr. My Yesterday"
        // After : "Hi! My Name is 'CustomMusicBot'!"

        // Real Usage (channelName = "I / Party Room"
        // r("I connected to `{voice_channel_name}`!", "{voice_channel_name}", channelName);
        // Before : "I connected to `{voice_channel_name}`!"
        // After : "I connected to `I / Party Room`!"

        for (int rl = 0; rl < rp.length; rl += 2) {
            str = str.replace(rp[rl], rp[rl + 1]);
            if (rl + 1 >= rp.length) {
                break;
            }
        }

        return str;

    }

    // -- Real Sources -- //

    public MusicListener() {
        this.musicManagers = new HashMap();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        try {

            sb = new StringBuffer();

            User user = event.getAuthor();
            Member member = event.getMember();
            TextChannel c = event.getChannel();
            Message msg = event.getMessage();
            Guild gld = event.getGuild();

            AudioManager am = gld.getAudioManager();

            String[] bannedUsers = new OptionReader().getBannedUsers();
            if (bannedUsers != null && new OptionReader().isEnabledUserBan()) {

                for (String bannedUser : bannedUsers) {
                    if (e.eq(user.getId(), bannedUser)) return;
                }

            }

            if (new OptionReader().isOwnerOnlyMode() && !e.eq(user.getId(), new OptionReader().getOwnerId())) {
                return;
            }

            if (user.isBot()) return;

            String[] args = msg.getContentRaw().substring(1).split(" ");
            char firstCharacter = msg.getContentRaw().charAt(0);

            if (firstCharacter == PREFIX) {

                if (member == null) return;
                if (args.length <= 0) return;

                if (e.eq(args[0], Command.CONNECT_COMMAND.getCommand())) {

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

                            c.sendMessage(r(CONNECT_VOICE_CHANNEL, "{voice_channel_name}", v.getName())).queue();

                        }

                    } catch (NullPointerException e) {
                        c.sendMessage(CANNOT_CONNECT_VOICE_CHANNEL).queue();
                    }

                }

                if (e.eq(args[0], Command.DISCONNECT_COMMAND.getCommand())) {

                    skipAllTrack(c, false, gld);
                    am.closeAudioConnection();
                    c.sendMessage(DISCONNECT_VOICE_CHANNEL).queue();

                }

                if (e.eq(args[0], Command.QUEUE_COMMAND.getCommand())) {

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

                            c.sendMessage(r(CONNECT_VOICE_CHANNEL, "{voice_channel_name}", v.getName())).queue();
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

                if (e.eq(args[0], Command.SKIP_COMMAND.getCommand())) {

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

                if (e.eq(args[0], Command.VOLUME_COMMAND.getCommand())) {

                    if (args.length == 1) return;

                    int vol = Integer.parseInt(args[1]);
                    setVolume(c, vol, true);

                }

                if (e.eq(args[0], Command.NOW_PLAYING_COMMAND.getCommand())) {
                    sendNowPlaying(c);
                }

                if (e.eq(args[0], Command.SHUFFLE_COMMAND.getCommand())) {
                    GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                    musicManager.scheduler.shuffle();

                    c.sendMessage(SHUFFLED_QUEUE).queue();
                }

                if (e.eq(args[0], Command.REPEAT_COMMAND.getCommand())) {
                    GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                    musicManager.scheduler.setRepeating(true);

                    c.sendMessage(musicManager.scheduler.isRepeating() ? ENABLED_REPEAT_TRACK : DISABLED_REPEAT_TRACK).queue();
                }

                if (e.eq(args[0], Command.SHOW_LIST_COMMAND.getCommand())) {
                    GuildMusicManager musicManager = getGuildAudioPlayer(c.getGuild());
                    TrackScheduler scheduler = musicManager.scheduler;

                    Queue<AudioInfo> queue = scheduler.queue;

                    if (queue.isEmpty()) {

                        c.sendMessage(QUEUE_EMPTY_MESSAGE).queue();

                    } else {

                        int trackCount = 0;
                        long queueLength = 0;
                        StringBuilder sb = new StringBuilder();

                        sb.append("```md\n# ").append(r(NOW_PLAYING_QUEUE_LIST, "{queue_amount}", String.valueOf(queue.size()))).append("\n\n");

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

                if (e.eq(args[0], Command.PLAYLISTS_COMMAND.getCommand())) {

                    if (args.length == 1) {

                        String[] urlLists = new DecupleAPI().getPlaylistElements(user);

                        for (String url : urlLists) {
                            loadAndPlay(c, url, false, member);
                        }

                        c.sendMessage(PLAY_PLAYLISTS.replace("{user}", user.getAsTag()).replace("{queue_amount}", String.valueOf(urlLists.length))).queue();
                        return;

                    }

                    if (e.eq(args[1], Command.SHOW_PLAYLISTS_COMMAND.getCommand())) {

                        StringBuilder sb = new StringBuilder("```md\n");
                        sb.append(SEND_PLAYLISTS_INFORMATION.replace("{user}", user.getAsTag()));

                        String[] urlLists = new DecupleAPI().getPlaylistElements(user);

                        for (int i = 0; i < urlLists.length; i++) {
                            sb.append("\n")
                                    .append(i)
                                    .append(". [")
                                    .append(new YoutubeAPI().getTitle(urlLists[i])
                                            .replace("[", "{")
                                            .replace("]", "}")
                                            .replace("_", "-")
                                            .replace("*", "-"))
                                    .append("](").append(urlLists[i]).append(urlLists[i].contains("_") ? ")" : ")");

                            if (sb.toString().length() > 1800) {
                                c.sendMessage(sb.append("```").toString()).queue();
                                sb = new StringBuilder("```md\n");
                            }
                        }

                        c.sendMessage(sb.append("```").toString()).queue();

                    }
                }

                if (e.eq(args[0], Command.SHUTDOWN_COMMAND.getCommand())) {
                    c.sendMessage(SHUTDOWN_BOT).queue();
                    System.exit(-1);
                }

            }
        } catch (StringIndexOutOfBoundsException ex) {
            // ignore
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    public void loadAndPlay(final TextChannel tc, String url, boolean showMessage, Member user) {

        sb = new StringBuffer();
        
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
                
                String timeStamp = r(TIME_STAMP, "{hour}", String.valueOf(hours), "{minitue}", String.valueOf(minutes), "{second}", String.valueOf(seconds));
                sb = new StringBuffer();
                EmbedBuilder eb = new EmbedBuilder();

                if (new OptionReader().isEnabledEmbedMessage()) {
                    eb.setDescription(r(ADDED_MUSIC_IN_QUEUE,
                            "{track_title}", audioTrack.getInfo().title,
                            "{track_channel}", audioTrack.getInfo().author,
                            "{track_duration}", timeStamp
                    ) + "(" + trackUrl + ")");
                    eb.setColor(Color.CYAN);

                    YoutubeAPI y = new YoutubeAPI();
                    eb.setImage(y.getThumbnail(trackUrl));

                    eb.setFooter(user.getUser().getAsTag(), user.getUser().getAvatarUrl());

                    if (showMessage) {
                        tc.sendMessage(eb.build()).queue();
                    }
                } else {
                    if (showMessage) {
                        tc.sendMessage("" + r(ADDED_MUSIC_IN_QUEUE, "{track_title}", audioTrack.getInfo().title,
                                "{track_channel}", audioTrack.getInfo().author,
                                "{track_duration}", timeStamp
                        ) + "(" + trackUrl + ")").queue();
                    }
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

                String timeStamp = r(TIME_STAMP, "{hour}", String.valueOf(hours), "{minitue}", String.valueOf(minutes), "{second}", String.valueOf(seconds));
                sb = new StringBuffer();
                EmbedBuilder eb = new EmbedBuilder();

                if (new OptionReader().isEnabledEmbedMessage()) {

                    eb.setDescription(r(ADDED_MUSIC_IN_QUEUE,
                            "{track_title}", firstTrack.getInfo().title,
                            "{track_channel}", firstTrack.getInfo().author,
                            "{track_duration}", timeStamp
                    ) + "(" + trackUrl + ")");
                    eb.setColor(Color.CYAN);

                    YoutubeAPI y = new YoutubeAPI();
                    eb.setImage(y.getThumbnail(trackUrl));

                    eb.setFooter(user.getUser().getAsTag(), user.getUser().getAvatarUrl());

                    if (showMessage) {
                        tc.sendMessage(eb.build()).queue();
                    }

                } else {

                    if (showMessage) {
                        tc.sendMessage(r(ADDED_MUSIC_IN_QUEUE,
                                "{track_title}", firstTrack.getInfo().title,
                                "{track_channel}", firstTrack.getInfo().author,
                                "{track_duration}", timeStamp
                        ) + "(" + trackUrl + ")").queue();
                    }

                }

                play(tc.getGuild(), musicManager, firstTrack, user, tc);
            }

            @Override
            public void noMatches() {
                EmbedBuilder eb = new EmbedBuilder();

                if (showMessage) {

                    eb.setDescription(CANNOT_FIND_SEARCH_RESULTS_IN_YOUTUBE);
                    eb.setColor(Color.RED);

                    if (new OptionReader().isEnabledEmbedMessage()) {
                        tc.sendMessage(eb.build()).queue();
                    } else {
                        tc.sendMessage(CANNOT_FIND_SEARCH_RESULTS_IN_YOUTUBE).queue();
                    }

                }
            }

            @Override
            public void loadFailed(FriendlyException e) {
                EmbedBuilder eb = new EmbedBuilder();

                if (showMessage) {
                    eb.setDescription(CANNOT_LOAD_TRACK);
                    eb.setColor(Color.RED);

                    if (new OptionReader().isEnabledEmbedMessage()) {
                        tc.sendMessage(eb.build()).queue();
                    } else {
                        tc.sendMessage(CANNOT_LOAD_TRACK).queue();
                    }
                }
            }
        });

    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {

        sb = new StringBuffer();
        
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
        
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
        musicManager.scheduler.nextTrack();

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setDescription(SKIP_TRACK);
            eb.setColor(Color.CYAN);

            if (new OptionReader().isEnabledEmbedMessage()) {
                tc.sendMessage(eb.build()).queue();
            } else {
                tc.sendMessage(SKIP_TRACK).queue();
            }
        }

    }

    public void skipTrack(TextChannel tc, int value, boolean showMessage) {

        sb = new StringBuffer();
        
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
        musicManager.scheduler.nextTrack(value);

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setDescription(r(SKIP_TRACKS, String.valueOf(value)));
            eb.setColor(Color.CYAN);

            if (new OptionReader().isEnabledEmbedMessage()) {
                tc.sendMessage(eb.build()).queue();
            } else {
                tc.sendMessage(r(SKIP_TRACKS, String.valueOf(value))).queue();
            }
        }

    }

    public void skipAllTrack(TextChannel tc, boolean showMessage, Guild guild) {
        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());

        sb = new StringBuffer();
        

        musicManager.pl.stopTrack();
        musicManager.scheduler.queue.clear();
        musicManager.pl.destroy();

        musicManagers.remove(guild.getIdLong());
        guild.getAudioManager().setSendingHandler(null);

        if (showMessage) {
            EmbedBuilder eb = new EmbedBuilder();

            eb.setDescription(SKIP_ALL_TRACKS);

            if (new OptionReader().isEnabledEmbedMessage()) {
                tc.sendMessage(eb.build()).queue();
            } else {
                tc.sendMessage(SKIP_ALL_TRACKS).queue();
            }
        }
    }

    public void setVolume(TextChannel tc, int volume, boolean showMessage) {
        sb = new StringBuffer();

        GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());

        int prVol = musicManager.scheduler.pl.getVolume();
        musicManager.scheduler.pl.setVolume(volume);

        EmbedBuilder eb = new EmbedBuilder();
        String message = null;

        if (prVol > volume) {
            eb.setColor(Color.ORANGE);
            eb.setTitle(SET_LOWER_VOLUME);
            message = SET_LOWER_VOLUME;
        } else if (prVol == volume) {
            eb.setColor(Color.YELLOW);
            showMessage = false;
        } else {
            eb.setColor(Color.CYAN);
            eb.setTitle(SET_HIGHER_VOLUME);
            message = SET_HIGHER_VOLUME;
        }
        eb.setDescription(prVol + " :arrow_forward: " + volume);

        if (!showMessage) return;

        if (new OptionReader().isEnabledEmbedMessage()) {
            tc.sendMessage(eb.build()).queue();
        } else
            tc.sendMessage(message + "(" + prVol + " :arrow_forward: " + volume + ")").queue();
    }

    public void sendNowPlaying(TextChannel tc) {
        sb = new StringBuffer();
        

        try {
            GuildMusicManager musicManager = getGuildAudioPlayer(tc.getGuild());
            AudioTrack audioTrack = musicManager.scheduler.pl.getPlayingTrack();

            AudioTrackInfo af = audioTrack.getInfo();
            EmbedBuilder eb = new EmbedBuilder();

            long as = audioTrack.getInfo().length;

            int seconds = (int) (as / 1000) % 60;
            int minutes = (int) ((as / (1000 * 60)) % 60);
            int hours = (int) ((as / (1000 * 60 * 60)));

            String timeStamp = r(TIME_STAMP, "{hour}", String.valueOf(hours), "{minitue}", String.valueOf(minutes), "{second}", String.valueOf(seconds));

            eb.setDescription(r(NOW_PLAYING_TRACK_INFO,
                    "{track_title}", audioTrack.getInfo().title,
                    "{track_channel}", audioTrack.getInfo().author,
                    "{track_duration}", timeStamp
            ) + "(" + af.uri + ")");

            if (new OptionReader().isEnabledEmbedMessage()) {
                tc.sendMessage(eb.build()).queue();
            } else {
                tc.sendMessage((r(NOW_PLAYING_TRACK_INFO,
                        "{track_title}", audioTrack.getInfo().title,
                        "{track_channel}", audioTrack.getInfo().author,
                        "{track_duration}", timeStamp
                ) + "(" + af.uri + ")")).queue();
            }
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

class OptionReader {

    static File optionFile = new File(BotBuilder.BASE_DIRECTORY + "/Config/SETTING.json");
    static JsonParser jp = new JsonParser();
    static JsonObject obj = (JsonObject) jp.parse(Objects.requireNonNull(new ReadFile().readString(optionFile)));

    public OptionReader() {

    }

    public boolean isAgreedLicense() {

        return optionFile.exists() && obj.get("license_agree").getAsString().equalsIgnoreCase("true");

    }

    public boolean isEnabledEmbedMessage() {

        return obj.get("showEmbedMessage").getAsString().equalsIgnoreCase("true");

    }

    public String getOwnerId() {

        return obj.get("ownerUserId").getAsString();

    }

    public boolean isOwnerOnlyMode() {

        return obj.get("canUseOnlyOwnerUser").getAsString().equalsIgnoreCase("true");

    }

    public boolean isEnabledUserBan() {

        return obj.get("enableUserBan").getAsString().equalsIgnoreCase("true");

    }

    public String[] getBannedUsers() {

        return getter("banList");

    }

    public static String[] getter(String get) {

        return obj.get(get).getClass() == JsonArray.class ?
                MusicListener.jsonArrayToStrArray(obj.get(get).getAsJsonArray()) :
                new String[] {obj.get(get).getAsJsonPrimitive().getAsString()};

    }

}

