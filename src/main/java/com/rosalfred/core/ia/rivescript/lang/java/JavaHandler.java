/**
 * This file is part of the Alfred package.
 *
 * (c) Mickael Gaillard <mick.gaillard@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.rosalfred.core.ia.rivescript.lang.java;

import java.util.HashMap;
import java.util.Map;

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
    protected Map<String, Object> objects = new HashMap<>();

    protected JavaHandler(RiveScript rs) {
        this.rs = rs;
    }

    @Override
    public BotReply onCall(String name, String user, String[] args) {
        BotReply result = new BotReply(name);
        // Creating an instance of our compiled class and
        // running its toString() method
        try {
            String method = null;

            if (name.endsWith("()")) {
                method = name.substring(name.lastIndexOf(".") + 1, name.length() - 2);
                name = name.substring(0, name.lastIndexOf("."));
            }

            Object instance = null;

            if (!this.objects.containsKey(name)) {
                ClassLoader loader = this.getClassLoader();
                Class<?> runtimeClass = loader.loadClass(name);

                if (method == null) {
                    instance = runtimeClass.newInstance();
                } else {
                    instance = runtimeClass.getConstructor(RiveScript.class).newInstance(this.rs);
                }
            } else {
                instance = this.objects.get(name);
            }

            if (method == null) {
                result = ((RunRiver) instance).invoke(this.rs, user, args);
            } else {
                result = (BotReply) instance.getClass().getMethod(method).invoke(instance);
            }
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
