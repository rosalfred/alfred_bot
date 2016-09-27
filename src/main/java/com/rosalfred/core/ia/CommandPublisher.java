/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia;

import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.Publisher;
import org.rosbuilding.common.IModule;

import com.rosalfred.core.ia.rivescript.RiveScript;

import smarthome_comm_msgs.msg.Command;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 *
 */
public abstract class CommandPublisher {

    protected final Object lockInstance = new Object();

    protected Node node;
    protected volatile RosRiveScript rivescript;
    protected Publisher<Command> publisher;

    public CommandPublisher(RiveScript rivescript) {
        this.rivescript = RosRiveScript.getRosRiveScript(rivescript);
        this.initialize();
    }

    /**
     * Initialize base of Command, connect to node and start publisher.
     * @param rivescript Instance of
     */
    protected synchronized void initialize() {
        if (node == null) {
            synchronized (this.lockInstance) {
                if (node == null) {
                    this.node       = this.rivescript.getNode();
                    this.publisher  = this.rivescript.getPublisherSay();

                    if ((this.node == null) || (this.publisher == null))
                        throw new RuntimeException();
                }
            }
        }
    }

    /**
     * Make and publish a generic command.
     * @param method
     */
    protected void publish(String method) {
        this.publish(method, "");
    }

    /**
     * Make and publish a generic command.
     * @param method
     */
    protected void publish(String method, String uri) {
        this.node.getLog().info("send sub command : " + method);

        Command message = new Command();
        message.setAction(IModule.SEP + method);
        message.setSubject(uri);

        this.publish(message);
    }

    /**
     * Publish a generic command.
     * @param msg
     */
    protected void publish(Command msg) {
        msg.getContext().setWho(IaNode.botname);
        msg.getContext().setWhere(  // TODO Mapping by knowedge relation
                this.getUserParam(IaNode.VAR_CONTEXT_WHERE));
        this.publisher.publish(msg);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) { }
    }

    /**
     * Get user parameter from context.
     * @param key Key identify
     * @return
     */
    protected String getUserParam(String key) {
        return this.rivescript.getUtils().getUserParam(key);
    }

    /**
     * Set a user parameter to context.
     * @param key
     * @param value
     */
    protected void setUserParam(String key, String value) {
        this.rivescript.getUtils().setUserParam(key, value);
    }
}
