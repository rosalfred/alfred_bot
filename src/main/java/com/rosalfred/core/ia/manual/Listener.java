package com.rosalfred.core.ia.manual;

import org.ros2.rcljava.RCLJava;
import org.ros2.rcljava.namespace.GraphName;
import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.topic.SubscriptionCallback;
import org.ros2.rcljava.node.topic.Subscription;

import com.rosalfred.core.ia.IaNode;

import smarthome_comm_msgs.msg.Command;

public class Listener {
    private static final String NODE_NAME = Listener.class.getName();

    public static void chatterCallback(Command msg) {
        System.out.println("Alfred say > " + msg.getSubject());
    }

    public static void main(String[] args) throws InterruptedException {
        // Initialize RCL
        RCLJava.rclJavaInit();

        // Let's create a new Node
        Node node = RCLJava.createNode(NODE_NAME);

        Subscription<Command> sub = node.<Command>createSubscription(
                Command.class,
                GraphName.getFullName(node, IaNode.PUB_STATE, null),
                new SubscriptionCallback<Command>() {
                    @Override
                    public void dispatch(Command msg) {
                        Listener.chatterCallback(msg);
                    }
                });

        RCLJava.spin(node);

        sub.dispose();
        node.dispose();
        RCLJava.shutdown();
    }
}
