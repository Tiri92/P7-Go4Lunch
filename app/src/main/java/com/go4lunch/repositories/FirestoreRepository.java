package com.go4lunch.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.firebase.ui.auth.AuthUI;
import com.go4lunch.model.firestore.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private static final String COLLECTION_NAME = "users";
    private static final String USERNAME_FIELD = "username";
    private static final String EATING_PLACE_ID = "eatingPlaceId";
    private static final String EATING_PLACE = "eatingPlace";
    private final MutableLiveData<List<User>> listOfUsers = new MutableLiveData<>();
    private final MutableLiveData<List<User>> listOfUsersWhoChoseRestaurant = new MutableLiveData<>();

    public FirestoreRepository() {
        getAllUsers();
    }

    // Get the Collection Reference
    public CollectionReference getUsersCollection() {
        return FirebaseFirestore.getInstance().collection(COLLECTION_NAME);
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return FirebaseAuth.getInstance().getCurrentUser();
    }

    public String getCurrentUserId() {
        return getCurrentUser().getUid();
    }

    public Task<Void> logout(Context context) {
        return AuthUI.getInstance().signOut(context);
    }

    public Task<Void> deleteUser(Context context) {
        return AuthUI.getInstance().delete(context);
    }

    /**
     * Firestore Request, CRUD action
     **/

    /* Create User in Firestore
       If user is authenticated, we try to get his data from Firestore with getUserData
       If getUserData fail, we create the user on Firebase
       If getUserData success but the user == null (doesn't exist in Firestore) we create it in Firebase
       And if the user of getUserData != null, so he already exist and we do nothing
    */
    public void createUser() {
        FirebaseUser user = getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            String username = user.getDisplayName();
            String email = (this.getCurrentUser().getEmail());
            String urlPicture = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
            String eatingPlace = " ";
            String eatingPlaceId = " ";

            User userToCreate = new User(uid, username, email, urlPicture, eatingPlace, eatingPlaceId);

            Task<DocumentSnapshot> userData = getUserData();

            userData.addOnFailureListener(documentSnapshot -> {
                this.getUsersCollection().document(uid).set(userToCreate);
            }).addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    User user = documentSnapshot.toObject(User.class);
                    if (user == null) {
                        getUsersCollection().document(uid).set(userToCreate);
                    }
                }
            });
        }
    }

    // Get All Users
    public void getAllUsers() {
        getUsersCollection().get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<User> allWorkMates = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                User myUser = document.toObject(User.class);

                                allWorkMates.add(myUser);
                            }
                            listOfUsers.setValue(allWorkMates);
                        } else {
                            Log.e("FirestoreRepository", "method getAllUsers don't work" + task.getException());
                        }
                    }
                });
    }

    public LiveData<List<User>> getListOfUsers() {
        return listOfUsers;
    }

    // Get Users who chose an eatingPlace
    public void getUsersWhoChoseRestaurant() {
        getUsersCollection()
                .whereNotEqualTo("eatingPlace", " ")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull @NotNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<User> allWorkMatesWhoChoseRestaurant = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {

                                User myUser = document.toObject(User.class);

                                allWorkMatesWhoChoseRestaurant.add(myUser);
                            }
                            listOfUsersWhoChoseRestaurant.setValue(allWorkMatesWhoChoseRestaurant);
                        } else {
                            Log.e("FirestoreRepository", "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    public LiveData<List<User>> getListOfUsersWhoChoseRestaurant() {
        getUsersWhoChoseRestaurant();
        return listOfUsersWhoChoseRestaurant;
    }

    // Get User Data from Firestore
    public Task<DocumentSnapshot> getUserData() {
        String uid = this.getCurrentUserId();
        if (uid != null) {
            return this.getUsersCollection().document(uid).get();
        } else {
            return null;
        }
    }

    public DocumentReference getUserDataForUpdate() {
        return getUsersCollection().document(getCurrentUserId());
    }

    // Update Username
    public Task<Void> updateUsername(String username) {
        String uid = this.getCurrentUserId();
        if (uid != null) {
            return this.getUsersCollection().document(uid).update(USERNAME_FIELD, username);
        } else {
            return null;
        }
    }

    // Update EatingPlaceId
    public Task<Void> updateEatingPlaceId(String eatingPlaceId) {
        String uid = this.getCurrentUserId();
        if (uid != null) {
            return this.getUsersCollection().document(uid).update(EATING_PLACE_ID, eatingPlaceId);
        } else {
            return null;
        }
    }

    //Update EatingPlace
    public Task<Void> updateEatingPlace(String eatingPlace) {
        String uid = this.getCurrentUserId();
        if (uid != null) {
            return this.getUsersCollection().document(uid).update(EATING_PLACE, eatingPlace);
        } else {
            return null;
        }
    }

    // Delete the User from Firestore
    public Task<Void> deleteUserFromFirestore(String userId) {
        return getUsersCollection().document(userId).delete();
    }

}
