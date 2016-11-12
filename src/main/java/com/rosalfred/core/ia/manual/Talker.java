package com.rosalfred.core.ia.manual;

import java.util.Scanner;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.Publisher;
import org.rosbuilding.common.media.CommandUtil;

import com.rosalfred.core.ia.IaNode;

import smarthome_comm_msgs.msg.Command;

public class Talker {

    public static void main(String[] args) throws InterruptedException {

        Scanner scanner = new Scanner(System.in);

        // Initialize RCL
        RCLJava.rclJavaInit();

        // Let's create a Node
        Node node = RCLJava.createNode("talker ia");

        Command msg = new Command();
        msg.getContext().setWho("Mickael");
        msg.getContext().setWhere("/home/salon");
        msg.setAction(CommandUtil.Action.SAY.getValue());
        msg.setSubject("");

        // Publishers are type safe, make sure to pass the message type
        Publisher<Command> chatter_pub =
                node.<Command>createPublisher(
                        Command.class,
                        GraphName.getFullName(node, IaNode.SUB_CMD, null));

        String value;
        while(RCLJava.ok()) {
            System.out.println("You >");
            value =scanner.nextLine();

            msg.setSubject(value);
            chatter_pub.publish(msg);
        }

        scanner.close();

        chatter_pub.dispose();
        node.dispose();
        RCLJava.shutdown();
    }
}
