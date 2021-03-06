/*
 * Copyright 2017 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.remoting3.okhttp;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class UserAgentInterceptor implements Interceptor {

    private static final Pattern VALID_USER_AGENT = Pattern.compile("[A-Za-z0-9()\\-#;/.,_\\s]+");
    private final String userAgent;

    private UserAgentInterceptor(String userAgent, Class<?> serviceClass) {
        Preconditions.checkArgument(VALID_USER_AGENT.matcher(userAgent).matches(),
                "User Agent must match pattern '%s': %s", VALID_USER_AGENT, userAgent);

        String version = getVersionString(serviceClass);
        this.userAgent = version.isEmpty()
                ? userAgent
                : userAgent + " (" + version + ")";
    }

    private String getVersionString(Class<?> serviceClass) {
        List<String> versions = new ArrayList<>(2);
        String maybeServiceVersion = serviceClass.getPackage().getImplementationVersion();
        if (maybeServiceVersion != null) {
            versions.add("Service API " + serviceClass.getSimpleName() + " " + maybeServiceVersion);
        }
        String maybeRemotingVersion = this.getClass().getPackage().getImplementationVersion();
        if (maybeRemotingVersion != null) {
            versions.add("http-remoting " + maybeRemotingVersion);
        }
        return Joiner.on(";").join(versions);
    }

    public static UserAgentInterceptor of(String userAgent, Class<?> serviceClass) {
        return new UserAgentInterceptor(userAgent, serviceClass);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", userAgent)
                .build();
        return chain.proceed(requestWithUserAgent);
    }
}
