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

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 *
 */
public class IaConfig { //extends BaseConfig {

    //public static final String RATE = "rate";
    public static final String RES_PATH = "res_path";

    public IaConfig(Node node) {
//        super(node);

        // TODO to custom
        //this.addField(RATE, "int", 0, "rate processus", "1", 0, 200);
//        this.addField(RES_PATH, "str", 0, "Path to brain", "/res", 0, 0);
    }
}
