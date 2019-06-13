/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.alauda.jenkins.plugins.credentials.convertor;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.Extension;
import io.alauda.jenkins.plugins.credentials.SecretUtils;
import io.kubernetes.client.models.V1Secret;

/**
 * SecretToCredentialConvertor that converts {@link UsernamePasswordCredentialsImpl}.
 */
@Extension
public class UsernamePasswordCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "kubernetes.io/basic-auth".equals(type);
    }

    @Override
    public UsernamePasswordCredentialsImpl convert(V1Secret secret) throws CredentialsConversionException {
        SecretUtils.requireNonNull(secret.getData(), "kubernetes.io/basic-auth definition contains no data");

        String username = SecretUtils.getNonNullSecretData(secret, "username", "kubernetes.io/basic-auth credential is missing the username");
        String password = SecretUtils.getNonNullSecretData(secret, "password", "kubernetes.io/basic-auth credential is missing the password");

        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), username, password);

    }

}
