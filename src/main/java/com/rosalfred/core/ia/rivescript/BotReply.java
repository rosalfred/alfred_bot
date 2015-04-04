package com.rosalfred.core.ia.rivescript;

import java.util.ArrayList;
import java.util.List;

public class BotReply {

    private String reply;
    private List<String> intents;

    public BotReply() {
        this("");
    }

    public BotReply(String reply) {
        this.reply = reply;
        this.intents = new ArrayList<String>();
    }

    public String getReply() {
        return this.reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public List<String> getIntents() {
        return intents;
    }

    @Override
    public String toString() {
        return this.reply;
    }
}
