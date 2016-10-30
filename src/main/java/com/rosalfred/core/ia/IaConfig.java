/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia;

import java.util.Arrays;

import org.ros2.rcljava.node.Node;
import org.ros2.rcljava.node.parameter.ParameterVariant;
import org.rosbuilding.common.NodeDriverConfig;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 *
 */
public class IaConfig extends NodeDriverConfig {

    public static final String PARAM_RES_PATH = "res_path";

    public IaConfig(Node node) {
        super(
                node,
                "/",
                "fixed_frame",
                1,
                "00:00:00:00:00:00");

        this.connectedNode.setParameters(
                Arrays.<ParameterVariant<?>>asList(
                        new ParameterVariant<String>(PARAM_RES_PATH,     "/res")
                ));
    }
}
