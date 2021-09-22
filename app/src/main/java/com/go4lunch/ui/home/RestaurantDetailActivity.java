package com.go4lunch.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.go4lunch.BuildConfig;
import com.go4lunch.R;
import com.go4lunch.databinding.ActivityRestaurantDetailBinding;
import com.go4lunch.model.User;
import com.go4lunch.model.details.SearchDetail;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RestaurantDetailActivity extends AppCompatActivity {

    private ActivityRestaurantDetailBinding binding;
    public RestaurantDetailViewModel restaurantDetailViewModel;
    String placeId;
    String nameOfCurrentRestaurant;

    private RecyclerView mRecyclerView;
    RecyclerView.Adapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRestaurantDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mRecyclerView = binding.restaurantDetailsRecyclerView;

        Intent intent = getIntent();
        placeId = intent.getStringExtra("placeId");
        nameOfCurrentRestaurant = intent.getStringExtra("name");

        restaurantDetailViewModel = new ViewModelProvider(this).get(RestaurantDetailViewModel.class);
        restaurantDetailViewModel.getListOfUsersWhoChoseRestaurant().observe(this, new Observer<List<User>>() {
            @Override
            public void onChanged(List<User> users) {
                mAdapter = new RestaurantDetailAdapter(users);
                mRecyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
                mRecyclerView.setAdapter(mAdapter);
            }
        });

        restaurantDetailViewModel.callRestaurantDetail(placeId);
        restaurantDetailViewModel.getSearchDetailResultFromVM().observe(this, new Observer<SearchDetail>() {
            @Override
            public void onChanged(SearchDetail searchDetail) {

                TextView restaurantName = binding.restaurantDetailsName;
                restaurantName.setText(searchDetail.getResult().getName());

                TextView restaurantAddress = binding.restaurantDetailsAddress;
                restaurantAddress.setText(searchDetail.getResult().getVicinity());

                ImageView restaurantPic = binding.restaurantDetailPic;
                try {
                    String base = "https://maps.googleapis.com/maps/api/place/photo?";
                    String key = "key=" + BuildConfig.MAPS_API_KEY;
                    String reference = "&photoreference=" + searchDetail.getResult().getPhotos().get(0).getPhotoReference();
                    String maxH = "&maxheight=157";
                    String maxW = "&maxwidth=157";
                    String query = base + key + reference + maxH + maxW;

                    Glide.with(restaurantPic)
                            .load(query)
                            .centerCrop()
                            .into(restaurantPic);

                } catch (Exception e) {
                    Log.i("[THIERRY]", "Exception : " + e.getMessage());
                }
            }
        });

        FloatingActionButton chosenRestaurantButton = binding.chosenRestaurantFloatingBtn;
        chosenRestaurantButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restaurantDetailViewModel.getUserData().addOnSuccessListener(new OnSuccessListener<User>() {
                    @Override
                    public void onSuccess(User user) {
                        restaurantDetailViewModel.updateEatingPlaceId(user.setEatingPlaceId(placeId));
                        restaurantDetailViewModel.updateEatingPlace(user.setEatingPlace(nameOfCurrentRestaurant));
                        showSnackBar(getString(R.string.success_chosen_restaurant));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull @NotNull Exception e) {
                        showSnackBar(getString(R.string.error_chosen_restaurant));
                    }
                });
            }
        });

    }

    // Show Snack Bar with a message
    private void showSnackBar(String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
    }
}

