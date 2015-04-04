/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia.rivescript;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.rosalfred.core.ia.rivescript.lang.Echo;
import com.rosalfred.core.ia.rivescript.lang.Java;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 *
 */
public class RSBot {
	public static void main (String[] args) {
		// Let the user specify debug mode!
		boolean debug = false;
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--debug") || args[i].equals("-d")) {
				debug = true;
			}
		}
		debug = true;

		// Create a new RiveScript interpreter.
		System.out.println(":: Creating RS Object");
		RiveScript rs = new RiveScript(debug);

		// Create a handler for X objects.
		rs.setHandler("perl", new Echo(rs)); //new com.rivescript.lang.Perl(rs, "./lang/rsp4j.pl"));
		rs.setHandler("python", new Echo(rs));
		rs.setHandler("java", new Java(rs, "/home/micky/ros-workspace/tact_medialfred/media_common/src/media_ia/res/src" ));

		// Load and sort replies
		System.out.println(":: Loading replies");
		rs.loadDirectory("/home/micky/ros-workspace/tact_medialfred/media_common/src/media_ia/res"); //"./Aiden");
		rs.sortReplies();

		// Enter the main loop.
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader stdin = new BufferedReader(converter);
		while (true) {
			System.out.print("You> ");
			String message = "";
			try {
				message = stdin.readLine();
			}
			catch (IOException e) {
				System.err.println("Read error!");
			}

			// Quitting?
			if (message.equals("/quit")) {
				System.exit(0);
			}
			else if (message.equals("/dump topics")) {
				rs.dumpTopics();
			}
			else if (message.equals("/dump sorted")) {
				rs.dumpSorted();
			}
			else {
			    String escapeString = message.replaceAll("-:", " "); // ignore
                String[] res = escapeString.split("[.,!?:;]+\\s*");

                String reply = "ERR : NOOOOOOO !";
                for (String sentence : res) {
                    reply = rs.reply("localuser", sentence).getReply();
                }

				System.out.println("Bot> " + reply);
			}
		}
	}
}
