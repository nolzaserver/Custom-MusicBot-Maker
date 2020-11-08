package com.projectdecuple;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.projectdecuple.Core.GetJSON;
import com.projectdecuple.Core.Utility.ReadFile;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Timer;
import java.util.TimerTask;

public class BotBuilder {

    public final static String BASE_DIRECTORY = System.getProperty("user.dir");
    private static final ReadFile r = new ReadFile();

    public final static String BOT_TOKEN = r.readString(BASE_DIRECTORY + "/TOKEN.txt");
    public final static String YOU_OAUTH = r.readString(BASE_DIRECTORY + "/YOUTUBE_API_KEY.txt");

    public final static String ACTIVITY = r.readString(BASE_DIRECTORY + "/Config/ACTIVITY.txt");
    public final static String NOW_VERSION = "v1.2.1";

    public static void main(String[] args) throws Exception {

        newerVersionCheckService();

        System.out.println("Working Directory : " + BASE_DIRECTORY);
        System.out.println("Custom-MusicBot-Maker by '데큐플#9999'. Custom-MusicBot-Maker was written based on the source code of QuintupleV2.");
        System.out.println("Now using version : " + NOW_VERSION);

        System.out.println(); // LF

        if (!new OptionReader().isAgreedLicense()) {
            System.out.println("You have not yet accepted the licenses.");
            System.out.println("To use this bot, you need to accept to the licenses.");

            System.out.println(); // LF

            System.out.println("ㅡㅡㅡㅡ How to accept to the licenses ㅡㅡㅡㅡ");
            System.out.println("1. Open the directory where this bot is located.");
            System.out.println("2. Open 'Config' folder.");
            System.out.println("3. Right-click 'SETTING.json'.");
            System.out.println("4. Click 'Edit'.");
            System.out.println("5. Modify the setting that says \"false\" in the \"license_agree\" section to \"true\".");
            return;
        }

        JDABuilder jdaBuilder = JDABuilder.createDefault(BOT_TOKEN)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_BANS,
                        GatewayIntent.GUILD_VOICE_STATES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS,
                        GatewayIntent.GUILD_EMOJIS)
                .setAutoReconnect(true)
                .setStatus(OnlineStatus.ONLINE)
                .addEventListeners(new MusicListener());

        JDA jda = jdaBuilder.build();
        jda.getPresence().setActivity((ACTIVITY != null) ? Activity.playing(ACTIVITY) : Activity.playing("Nothing"));
    }

    public static void newerVersionCheckService() throws Exception {
        String versionInfo = new GetJSON().getJsonByUrl("http://192.168.219.102:7777/custom-musicbot-maker");

        JsonParser jp = new JsonParser();
        JsonObject info = (JsonObject) jp.parse(versionInfo);
        String version = info.get("nowVersion").getAsString();

        if (!version.equalsIgnoreCase(NOW_VERSION) && new OptionReader().isCheckedAutoUpdate()) {
            updateVersion(info.get("downloadLink").getAsString(), version);
        }
    }

    public static void updateVersion(String url, String version) throws IOException {

        if (!new File(BASE_DIRECTORY + "/..UPDATE/Custom-MusicBot-Maker-" + version + ".zip").exists()) {

            URL website = new URL(url);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream("..UPDATE/Custom-MusicBot-Maker-" + version + ".zip");

            Timer t = new Timer();
            System.out.println("Downloading 'Custom-MusicBot-Maker'. (new version of release)");
            System.out.print("Wait a moment");

            t.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    System.out.print(".");
                }
            }, 0, 3000);

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            t.cancel();

        } else {

            System.out.println("Oops, latest released file exists in download directory already. Sorry. You can use this program now.");
            return;

        }

        System.out.println("\n\nOkay! You can use this program now.");
    }
}