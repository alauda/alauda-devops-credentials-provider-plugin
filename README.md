# Alauda DevOps Credentials Provider plugin


This plugin will sync secrets from Kubernetes to Jenkins and convert them to Credentials stored in memory.

It provides four extension points for other plugins to implement to have more powerful functionality.

1. SecretToCredentialConverter defines which type of secret should be convert and how to convert it.
2. MetadataProvider allows adding metadata to Credentials
3. KubernetesSecretRule defines which secret should 