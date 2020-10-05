package com.projectdecuple;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectdecuple.Core.Utility.EasyEqual;
import com.projectdecuple.Core.Utility.ReadFile;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.Objects;

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

    public String CONNECT_COMMAND = command.get("connectCommand").getAsString();
    public String DISCONNECT_COMMAND = command.get("disconnectCommand").getAsString();
    public String QUEUE_COMMAND = command.get("queueCommand").getAsString();

    public char PREFIX = command.get("prefix").getAsString().charAt(0);

    public static final EasyEqual e = new EasyEqual();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

        User user = event.getAuthor();
        Member member = event.getMember();
        TextChannel c = event.getChannel();
        Message msg = event.getMessage();
        Guild gld = event.getGuild();

        StringBuilder sb = new StringBuilder();
        Formatter f = new Formatter(sb);

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

                // TODO : Skip All Tracks
                am.closeAudioConnection();
                c.sendMessage(DISCONNECT_VOICE_CHANNEL).queue();

            }

            if (e.eq(args[0], QUEUE_COMMAND)) {



            }

        }

    }

}

