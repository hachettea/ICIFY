

public class Server
{

    public static void main(String[] args)
    {
        Server serveur = new Server();
        serveur.run(args);
    }

    private void run(String[] args)
    {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<String>();

        try(com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.server", extraArgs))
        {
            communicator.getProperties().setProperty("Ice.Default.Package", "com.zeroc.demos.Ice.callback");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> communicator.destroy()));

            if(!extraArgs.isEmpty())
            {
                System.err.println("too many arguments");
                status = 1;
            }
            else
            {
                com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("Callback.Server");
                adapter.add(new CallbackSenderI(), com.zeroc.Ice.Util.stringToIdentity("callbackSender"));
                adapter.activate();

                communicator.waitForShutdown();
            }
        }
        System.exit(status);
    } 
}
