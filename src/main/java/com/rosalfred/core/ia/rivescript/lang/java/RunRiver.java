package com.rosalfred.core.ia.rivescript.lang.java;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.RiveScript;

public interface RunRiver {
    public BotReply invoke(RiveScript rs, String user, String[] args);

    public class TestEcho implements RunRiver {
        @Override
        public BotReply invoke(RiveScript rs, String user, String[] args) {
            return new BotReply("echo > TestEcho from " + user);
        }
    }
}