/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia.rivescript.lang.java;

import com.rosalfred.core.ia.rivescript.BotReply;
import com.rosalfred.core.ia.rivescript.ObjectHandler;
import com.rosalfred.core.ia.rivescript.RiveScript;

/**
*
* @author Erwan Le Huitouze <erwan.lehuitouze@gmail.com>
*
*/
public abstract class JavaHandler implements ObjectHandler<BotReply> {

    protected RiveScript rs;

    protected JavaHandler(RiveScript rs) {
        this.rs = rs;
    }

    @Override
    public BotReply onCall(String name, String user, String[] args) {
        BotReply result = new BotReply(name);
        // Creating an instance of our compiled class and
        // running its toString() method
        try {
            ClassLoader loader = this.getClassLoader();
            Class<?> runtimeClass = loader.loadClass(name);
            RunRiver instance = (RunRiver) runtimeClass.newInstance();
            result = instance.invoke(this.rs, user, args);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (ClassFormatError e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    protected ClassLoader getClassLoader() {
        return this.getClass().getClassLoader();
    }
}
