/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia;

import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;

import com.rosalfred.core.ia.rivescript.RiveScript;

import building_msgs.Command;


/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 *
 */
public class RosRiveScript extends RiveScript {

    private final IaNode iaNode;
    private final RsContext rsutils;

    public RosRiveScript(IaNode iaNode, boolean debug) {
        super(debug);

        this.iaNode = iaNode;
        this.rsutils = new RsContext(this);
    }

    public ConnectedNode getNode() {
        return this.iaNode.connectedNode;
    }

    /**
     * @return the publisherSay
     */
    public Publisher<Command> getPublisherSay() {
        return this.iaNode.publisherSay;
    }

    public RsContext getUtils() {
        return this.rsutils;
    }

    public static RosRiveScript getRosRiveScript(RiveScript rs) {
        RosRiveScript result = null;

        if (rs.getClass() == RosRiveScript.class) {
            result = (RosRiveScript) rs;
        } else {
            result = null ;new MockRosRivescript(true);
        }

        return result;

    }

    private static class MockRosRivescript extends RosRiveScript {

        public MockRosRivescript(boolean debug) {
            super(null, debug);
        }
    }

}