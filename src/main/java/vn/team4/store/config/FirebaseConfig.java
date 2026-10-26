package vn.team4.store.config;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Date;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp(@Value("${app.firebase.enabled:false}") boolean firebaseEnabled) {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options;
        try {
            GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
            options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(firebaseEnabled ? "demostore-dce10" : "local-project")
                    .build();
        } catch (IOException ex) {
            GoogleCredentials fallbackCredentials = GoogleCredentials.create(
                    new AccessToken("test-token", new Date(System.currentTimeMillis() + 60_000L))
            );
            options = FirebaseOptions.builder()
                    .setCredentials(fallbackCredentials)
                    .setProjectId("local-project")
                    .build();
        }

        return FirebaseApp.initializeApp(options);
    }

    @Bean
    public Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }
}