import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
 
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


import com.zeroc.demos.Ice.callback.Demo.*;

public final class CallbackSenderI implements CallbackSender
{
    
    static ArrayList<Musique> listeMusique = new ArrayList<Musique>();
    static String musique;

    @Override
    public void initiateCallback(CallbackReceiverPrx proxy,byte[] fichier, com.zeroc.Ice.Current current)
    {
        String finalPath = System.getProperty("user.dir") + "\\commandeNew.wav";

        System.out.println(Arrays.toString(fichier));
        try {
            Files.write(Paths.get(finalPath), fichier);
        } 
        catch (IOException e) {

        }
        recuperListeDesMusiques();
        try
        {
            proxy.callback(fichier);
        }
        catch(com.zeroc.Ice.LocalException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void shutdown(com.zeroc.Ice.Current current)
    {
        System.out.println("Shutting down...");
        try
        {
            current.adapter.getCommunicator().shutdown();
        }
        catch(com.zeroc.Ice.LocalException ex)
        {
            ex.printStackTrace();
        }
    }

    private static void recuperListeDesMusiques() 
    {
        //JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
         
        try (FileReader reader = new FileReader("bdd.json"))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
 
            JSONArray musiqueList = (JSONArray) obj;
            System.out.println(musiqueList);
             
            //Iterate over employee array
            musiqueList.forEach( emp -> parseMusiqueObject( (JSONObject) emp ) );
            new CallbackSenderI();
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static void parseMusiqueObject(JSONObject musique) 
    {
        JSONObject musiqueObject = (JSONObject) musique.get("musique");
        
        String picture = (String) musiqueObject.get("picture");    
        System.out.println(picture);
         
        String title = (String) musiqueObject.get("title");    
        System.out.println(title);
         
        String artist = (String) musiqueObject.get("artiste");    
        System.out.println(artist);

        String album = (String) musiqueObject.get("album");    
        System.out.println(album);
        listeMusique.add(new Musique(picture,title,artist,album));
    }
}
