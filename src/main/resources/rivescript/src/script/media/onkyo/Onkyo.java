/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package script.media.onkyo;

import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.Publisher;
import org.rosbuilding.common.System;
import org.rosbuilding.common.media.CommandUtil;
import org.rosbuilding.common.media.Speaker;

import com.rosalfred.core.ia.IaNode;
import com.rosalfred.core.ia.RosRiveScript;

import smarthome_comm_msgs.msg.Command;

/**
 *
 * @author Erwan Lehuitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class Onkyo {

    private static Object lockInstance = new Object();
    private static Node node;
    private static Publisher<Command> publisher;

    private final RosRiveScript rivescript;

    public Onkyo(RosRiveScript rivescript) {
        this.rivescript = rivescript;
        this.initialize(rivescript);
    }

    private void initialize(RosRiveScript rivescript) {
        if (node == null) {
            synchronized (lockInstance) {
                if (node == null) {
                    node = rivescript.getNode();
                    publisher = rivescript.getPublisherSay();
                }
            }
        }
    }

    private void publish(String method) {
        node.getLog().info("send sub command : " + method);

        Command message = new Command(); // node.getTopicMessageFactory().newFromType(Command._TYPE);
        String user = this.rivescript.getCurrentUser();
        message.getContext().setWho(IaNode.botname);
        message.getContext().setWhere(  // TODO Mapping by knowedge relation
                this.rivescript.getUservar(user, IaNode.VAR_CONTEXT_WHERE));
        message.setAction("::" + method);
        publisher.publish(message);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void startReceiver() {
        this.publish(System.OP_POWER);

        Command command = CommandUtil.toCommand(
                node,
                Speaker.OP_CHANNEL,
                "SLI10");

        publisher.publish(command);
    }

    public void shutdownReceiver() {
        this.publish(System.OP_SHUTDOWN);
    }

    public void mute() {
        this.publish(Speaker.OP_MUTE);
    }
}
