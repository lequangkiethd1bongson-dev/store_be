package vn.team4.store.repository;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Repository;
import vn.team4.store.model.User;

@Repository("firestoreUserRepository")
public class UserRepository {
    private final Firestore firestore;

    public UserRepository(Firestore firestore) {
        this.firestore = firestore;
    }

    public User findById(String id) throws Exception {
        DocumentSnapshot document = firestore
                .collection("user")
                .document(id)
                .get()
                .get();

        if (!document.exists()) {
            return null;
        }

        User user = document.toObject(User.class);
        if (user != null) {
            // user.setId(document.getId());
        }
        return user;
    }
}
