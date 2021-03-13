//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

import com.zeroc.demos.Ice.callback.Demo.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Client {
    public static void main(String[] args) {
        int status = 0;
        java.util.List<String> extraArgs = new java.util.ArrayList<>();

        try (com.zeroc.Ice.Communicator communicator = com.zeroc.Ice.Util.initialize(args, "config.client",
                extraArgs)) {
            communicator.getProperties().setProperty("Ice.Default.Package", "com.zeroc.demos.Ice.callback");

            if (!extraArgs.isEmpty()) {
                System.err.println("too many arguments");
                status = 1;
            } else {
                status = run(communicator);
            }
        }

        System.exit(status);
    }

    private static int run(com.zeroc.Ice.Communicator communicator) {
        CallbackSenderPrx sender = CallbackSenderPrx.checkedCast(communicator.propertyToProxy("CallbackSender.Proxy"))
                .ice_twoway().ice_timeout(-1).ice_secure(false);
        if (sender == null) {
            System.err.println("invalid proxy");
            return 1;
        }

        com.zeroc.Ice.ObjectAdapter adapter = communicator.createObjectAdapter("Callback.Client");
        adapter.add(new CallbackReceiverI(), com.zeroc.Ice.Util.stringToIdentity("callbackReceiver"));
        adapter.activate();

        CallbackReceiverPrx receiver = CallbackReceiverPrx
                .uncheckedCast(adapter.createProxy(com.zeroc.Ice.Util.stringToIdentity("callbackReceiver")));

        String path = System.getProperty("user.dir") + "\\commande.wav";
        System.out.println(path);
        byte[] encoded = null;
        try {
            encoded = Files.readAllBytes(Paths.get(path));
            System.out.println(Arrays.toString(encoded));
        } catch (IOException e) {

        }
        sender.initiateCallback(receiver, encoded);

        // quand on quittera l'applciation android
        // sender.shutdown();
        return 0;
    }

}
