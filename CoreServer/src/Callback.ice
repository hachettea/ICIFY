//
// Copyright (c) ZeroC, Inc. All rights reserved.
//

#pragma once

["java:package:com.zeroc.demos.Ice.callback"]



module Demo
{
    sequence<byte> Fichier;
    interface CallbackReceiver
    {
        void callback(Fichier fichier);
    }
    interface CallbackSender
    {
        void initiateCallback(CallbackReceiver* proxy,Fichier fichier);
        void shutdown();
    }
}

