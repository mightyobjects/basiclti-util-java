/*
 * Copyright 2018 IMS Global Learning Consortium.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.imsglobal.lti2;

import com.google.inject.AbstractModule;
import com.mastfrog.acteur.Acteur;
import com.mastfrog.acteur.Application;
import com.mastfrog.acteur.HttpEvent;
import com.mastfrog.acteur.Page;
import static com.mastfrog.acteur.headers.Method.GET;
import static com.mastfrog.acteur.headers.Method.POST;
import static com.mastfrog.acteur.headers.Method.PUT;
import com.mastfrog.acteur.preconditions.Methods;
import com.mastfrog.acteur.preconditions.PathRegex;
import com.mastfrog.acteur.server.PathFactory;
import com.mastfrog.acteur.server.ServerModule;
import static com.mastfrog.acteur.server.ServerModule.PORT;
import com.mastfrog.acteur.util.Server;
import com.mastfrog.giulius.Dependencies;
import com.mastfrog.settings.Settings;
import com.mastfrog.settings.SettingsBuilder;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 *
 * @author Tim Boudreau
 */
public class SampleApp extends Application { //using the old api so we don't pollute the classpath

    SampleApp() {
        add(SamplePage.class);
    }

    @PathRegex({"^sample\\/.*$", "^sample\\/?$"})
    @Methods({GET, PUT, POST})
    static final class SamplePage extends Page {

        SamplePage() {
            add(SampleActeur.class);
        }

        static final class SampleActeur extends Acteur {

            @Inject
            SampleActeur(LTI2Servlet lti, HttpEvent evt) throws IOException {
                lti.onRequest(evt, response());
                setState(new ConsumedState());
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Settings settings = new SettingsBuilder()
                .add(PORT, 8080)
                .build();

        new Dependencies(settings, new ServerModule(SampleApp.class)).getInstance(Server.class)
                .start().await();
    }

    static class M extends AbstractModule {

        @Override
        protected void configure() {
            bind(LTI2Servlet.class).toProvider(LtiServletProvider.class);
        }

    }

    static final class LtiServletProvider implements Provider<LTI2Servlet> {

        private final PathFactory pf;
        private final Settings settings;

        LtiServletProvider(PathFactory pf, Settings settings) {
            this.pf = pf;
            this.settings = settings;
        }

        @Override
        public LTI2Servlet get() {
            String secret = settings.getString("secret", "secret");
            String key = settings.getString("key", "key");
            return new LTI2Servlet(pf, settings, key, secret);
        }

    }
}
