/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia;

import com.rosalfred.core.ia.rivescript.RiveScript;

/**
 *
 * @author Mickael Gaillard <mick.gaillard@gmail.com>
 * @author Erwan Le Huitouze <erwan.lehuitouze@gmail.com>
 *
 */
public class RsContext {
	private final RiveScript rs;

	public final static String FIND    = "find";
	public final static String FIND2   = "find2";
	public final static String FIND3   = "find3";
	public final static String FINDING = "finding";
	public final static String PAGE    = "page";
	public final static String LAUNCH  = "launch";
	public final static String COUNT   = "count";
	public final static String RESUME  = "resume";

	int step = 5;
	int page = 0;
	int count = -1;
	//args = {}

	public RsContext(RiveScript riveScript) {
		this.rs = riveScript;
	}

	public String getUserParam(String name) {
		String user = this.rs.getCurrentUser();
		return this.rs.getUservar(user, name);
	}

	public void setUserParam(String name, String value) {
		String user = this.rs.getCurrentUser();
		this.rs.setUservar(user, name, value);
	}

	public int getIndexItemPage() {
		return (this.step * this.page);
	}

	public int getStep() {
		return this.step;
	}

	public void checkPage() {
		String value = this.getUserParam(PAGE);
		if (value != null && !value.equals("")) {
	        this.page = Integer.parseInt(value);

            // Check negatif value
            if (this.page < 0) {
                this.page = 0;
            }

            // Check min step
            if (this.count > 0 && this.count < this.step) {
                this.page = 0;
            }
		}
	}

	public static String normalize(String value) {
	    return value.replace("/[^A-Za-z0-9 ]/", "");
	}
}
