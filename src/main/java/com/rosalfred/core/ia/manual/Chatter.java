package com.rosalfred.core.ia.manual;

import java.util.Scanner;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.SubscriptionCallback;
import org.ros2.rcljava.node.topic.Publisher;
import org.ros2.rcljava.node.topic.Subscription;
import org.rosbuilding.common.media.CommandUtil;

import com.rosalfred.core.ia.IaNode;

import smarthome_comm_msgs.msg.Command;

public class Chatter {

    public static void chatterCallback(Command msg) {
        System.out.println("> " + msg.getSubject());
    }

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        // Initialize RCL
        RCLJava.rclJavaInit();

        // Let's create a Node
        final Node node = RCLJava.createNode("talker ia");

        Command msg = new Command();
        msg.getContext().setWho("Mickael");
        msg.getContext().setWhere("/home/salon");
        msg.setAction(CommandUtil.Action.SAY.getValue());
        msg.setSubject("");

        Publisher<Command> chatter_pub = node.<Command>createPublisher(
                        Command.class,
                        GraphName.getFullName(node, IaNode.SUB_CMD, null));

        Subscription<Command> sub = node.<Command>createSubscription(
                        Command.class,
                        GraphName.getFullName(node, IaNode.PUB_STATE, null),
                        new SubscriptionCallback<Command>() {
                            @Override
                            public void dispatch(Command msg) {
                                Listener.chatterCallback(msg);
                            }
                        });

        String value;

        // Async Listener
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                RCLJava.spin(node);
            }
        });
        th.start();

        // Talker
        while(RCLJava.ok()) {
            value =scanner.nextLine();

            msg.setSubject(value);
            chatter_pub.publish(msg);
        }

        scanner.close();
        chatter_pub.dispose();
        sub.dispose();
        node.dispose();
        RCLJava.shutdown();
    }


}
