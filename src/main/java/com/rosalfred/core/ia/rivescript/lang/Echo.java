package com.rosalfred.core.ia.rivescript.lang;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.ObjectHandler;
import com.rosalfred.core.ia.rivescript.RiveScript;

public class Echo implements ObjectHandler<BotReply> {

    public Echo(RiveScript rs) {

    }

    @Override
    public boolean onLoad(String name, String[] code) {
        return false;
    }

    @Override
    public BotReply onCall(String name, String user, String[] args) {
        return new BotReply(name);
    }

}
