package com.jmgarzo.udacity.popularmovies.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

import com.jmgarzo.udacity.popularmovies.DetailFragment;
import com.jmgarzo.udacity.popularmovies.Objects.Movie;

/**
 * Created by jmgarzo on 24/03/17.
 */

public class AddTrailerAndReviewIntentService extends IntentService {

    public AddTrailerAndReviewIntentService(){super("AddTrailerAndReviewIntentService");}

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Movie movie = intent.getParcelableExtra(DetailFragment.MOVIE_TAG);
        PopularMoviesSyncTask.addTrailersAndReviews(this,movie);
    }
}
