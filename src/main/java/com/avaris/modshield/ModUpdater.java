package com.avaris.modshield;

import net.fabricmc.loader.api.FabricLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

/**
 * This class retrieves the latest mod version from the server<br>
 * @see ModUpdater#downloadLatest()
 */
public class ModUpdater {

    private static final String SERVER_URL = "https://vps-a25564b7.vps.ovh.net/";
    private static final String PATH = "com/avaris/"+ModShield.MOD_ID_CAP+"/";
    private static final String METADATA_FILE = "maven-metadata.xml";

    /**
     * Creates the download URL from the version string.
     * @param version a String of the version
     * @return a String of the download URL
     */
    public static String getDownloadUrl(String version){
        return SERVER_URL + PATH + version +"/" + ModShield.MOD_ID_CAP+"-"+ version +".jar";
    }

    /**
     * Checks for the latest mod version. And attempts to download it if autoUpdate is enabled.<br>
     * Otherwise logs that the mod is outdated it applicable.
     */
    public static void downloadLatest(){
        String latestVersion = getLatestVersion();
        if(latestVersion == null){
            return;
        }
        if(Objects.equals(latestVersion, ModShield.MOD_VERSION)){
            ModShield.getLogger().info("{} v{}:{} is up to date",ModShield.MOD_ID_CAP,ModShield.MOD_VERSION,ModShield.PROTOCOL_VERSION);
           return;
        }
        if(!ShieldConfig.shouldAutoUpdate()){
            ModShield.getLogger().info("{} is out of date; Current version: {} latest version: {}\nPlease Download the latest version: {}",
                    ModShield.MOD_ID_CAP,ModShield.MOD_VERSION,latestVersion,getDownloadUrl(latestVersion));
           return;
        }
        ModShield.getLogger().info("Attempting autoupdate to v{}",latestVersion);

        HttpClient client = HttpClient.newHttpClient();
        try{
            client.send(HttpRequest.newBuilder()
                    .GET().uri(URI.create(getDownloadUrl(latestVersion))).build(),
                    HttpResponse.BodyHandlers.ofFile(FabricLoader.getInstance().getGameDir().resolve("mods/"+ModShield.MOD_ID_CAP+"-"+latestVersion+".jar")));
            ModShield.getLogger().info("Autoupdate successful, please restart the game delete mods/{}-{}.jar",ModShield.MOD_ID_CAP,ModShield.MOD_VERSION);
        }catch (Exception e){
            ModShield.getLogger().info("Autoupdate failed with error:");
            e.printStackTrace();
        }
        client.close();

    }

    /**
     * Retrieves the latest version of the mod from the server.
     * @return a String of the latest version or null if an error occurred.
     */
    public static String getLatestVersion(){
        try{
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final Document document = factory.newDocumentBuilder().parse(SERVER_URL + PATH + METADATA_FILE);
            final NodeList nodes = document.getDocumentElement().getElementsByTagName("versioning").item(0).getChildNodes();
            String latest = "";
            for(int i = 0;;i++){
                final Node node = nodes.item(i);
                if(node == null){
                    break;
                }
                if(node.getChildNodes().item(0) != null){
                    String value = node.getChildNodes().item(0).getNodeValue();
                    if(node.getNodeName().equals("latest")) {
                        latest = value;
                    }
                }
            }
            return latest;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}