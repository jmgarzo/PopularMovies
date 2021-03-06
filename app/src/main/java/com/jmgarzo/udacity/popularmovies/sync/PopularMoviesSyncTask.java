package com.jmgarzo.udacity.popularmovies.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.jmgarzo.udacity.popularmovies.Objects.Movie;
import com.jmgarzo.udacity.popularmovies.Objects.Review;
import com.jmgarzo.udacity.popularmovies.Objects.Trailer;
import com.jmgarzo.udacity.popularmovies.data.PopularMovieContract;
import com.jmgarzo.udacity.popularmovies.utilities.DataBaseUtils;
import com.jmgarzo.udacity.popularmovies.utilities.NetworksUtils;

import java.util.ArrayList;

import static com.jmgarzo.udacity.popularmovies.data.PopularMovieContract.MovieEntry.MOVIE_WEB_ID;

/**
 * Created by jmgarzo on 17/03/17.
 */

/**
 * Conteint This class conteint all sync task
 */
public class PopularMoviesSyncTask {


    /**
     * This method receive a movie that is in database but with registry type value top_rated or most_popular.
     * That method create a new registry with the same values for favorite to avoid be delete.
     * @param context
     * @param movie
     */
    public static void addFavorite(Context context, Movie movie) {

        movie.setRegistryType(PopularMovieContract.FAVORITE_REGISTRY_TYPE);

        Uri insertResultUri = context.getContentResolver().insert(
                PopularMovieContract.MovieEntry.CONTENT_URI,
                movie.getContentValues());

        String newIdMovie = null;
        if (insertResultUri != null) {
            newIdMovie = insertResultUri.getLastPathSegment();
        }

        if (newIdMovie != null) {
            addTrailersToFavorite(context, movie, newIdMovie);
            addReviewsToFavorite(context, movie, newIdMovie);
        }


    }

    /**
     * This method add all trailers movie to favorite.
     * @param context
     * @param movie
     * @param newIdMovie
     */
    private static void addTrailersToFavorite(Context context, Movie movie, String newIdMovie) {
        String selection = PopularMovieContract.TrailerEntry.MOVIE_KEY + " = ? ";

        Cursor trailerCursor = context.getContentResolver().query(
                PopularMovieContract.TrailerEntry.CONTENT_URI,
                DataBaseUtils.TRAILER_COLUMNS,
                selection,
                new String[]{Integer.toString(movie.getId())},
                null);

        if (trailerCursor.moveToFirst()) {

            ContentValues[] trailerContentValues = new ContentValues[trailerCursor.getCount()];
            for (int i = 0; i < trailerCursor.getCount(); i++) {
                Trailer trailer = new Trailer(trailerCursor, i);
                //We change
                trailer.setMovieKey(Integer.valueOf(newIdMovie));
                trailer.setRegistryType(PopularMovieContract.FAVORITE_REGISTRY_TYPE);
                trailerContentValues[i] = trailer.getContentValues();
            }

            context.getContentResolver().bulkInsert(
                    PopularMovieContract.TrailerEntry.CONTENT_URI,
                    trailerContentValues);
        }
    }

    private static void addReviewsToFavorite(Context context, Movie movie, String newIdMovie) {
        String selection = PopularMovieContract.ReviewEntry.MOVIE_KEY + " = ? ";

        Cursor reviewCursor = context.getContentResolver().query(
                PopularMovieContract.ReviewEntry.CONTENT_URI,
                DataBaseUtils.REVIEW_COLUMNS,
                selection,
                new String[]{Integer.toString(movie.getId())},
                null);

        if (reviewCursor.moveToFirst()) {

            ContentValues[] reviewContentValues = new ContentValues[reviewCursor.getCount()];
            for (int i = 0; i < reviewCursor.getCount(); i++) {
                Review review = new Review(reviewCursor, i);
                review.setMovieKey(Integer.valueOf(newIdMovie));
                review.setRegistryType(PopularMovieContract.FAVORITE_REGISTRY_TYPE);
                reviewContentValues[i] = review.getContentValues();
            }

            context.getContentResolver().bulkInsert(
                    PopularMovieContract.ReviewEntry.CONTENT_URI,
                    reviewContentValues);
        }
    }

    public static void deleteFromFavorite(Context context, Movie movie) {

        String selection = MOVIE_WEB_ID + " = ?  AND "
                + PopularMovieContract.MovieEntry.REGISTRY_TYPE + " = ?";
        String[] selectionArgs = {movie.getMovieWebId(), PopularMovieContract.FAVORITE_REGISTRY_TYPE};

        context.getContentResolver().delete(
                PopularMovieContract.MovieEntry.CONTENT_URI,
                selection,
                selectionArgs);
    }


    /**
     * This method make a new ask to API to get new data movies, and delete the old records
     * except favorite.
     * @param context
     */
    synchronized public static void syncMovies(Context context) {

        try {


            ArrayList<Movie> moviesList = NetworksUtils.getMovies(context);

            if (moviesList != null && moviesList.size() > 0) {
                ContentValues[] contentValues = new ContentValues[moviesList.size()];
                for (int i = 0; i < moviesList.size(); i++) {
                    Movie movie = moviesList.get(i);
                    contentValues[i] = movie.getContentValues();
                }

                ContentResolver contentResolver = context.getContentResolver();
                contentResolver.delete(
                        PopularMovieContract.MovieEntry.CONTENT_URI,
                        PopularMovieContract.MovieEntry.REGISTRY_TYPE + " <> ? ",
                        new String[]{PopularMovieContract.FAVORITE_REGISTRY_TYPE});

                contentResolver.bulkInsert(PopularMovieContract.MovieEntry.CONTENT_URI,
                        contentValues);

                //We can't update all trailer and review here because the API have a limitation.
                //syncTrailersAndReviews(context);


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*  We used this method to sync all trailers and Reviews before, but the API have a
    Request Rate Limiting and we decided to load new trailers and reviews within MovieGridViewAdapter.
    */
//    private static void syncTrailersAndReviews(Context context) {
//
//        try {
//
//
//            //We update all movies trailers in DB, favorite's trailers too.
//            String[] projection = {PopularMovieContract.MovieEntry._ID,
//                    MOVIE_WEB_ID,
//                    PopularMovieContract.MovieEntry.REGISTRY_TYPE};
//
//            Cursor moviesCursor = context.getContentResolver().query(
//                    PopularMovieContract.MovieEntry.CONTENT_URI,
//                    projection,
//                    null,
//                    null,
//                    null);
//
//            if (moviesCursor != null && moviesCursor.moveToFirst()) {
//                context.getContentResolver().delete(
//                        PopularMovieContract.TrailerEntry.CONTENT_URI,
//                        null,
//                        null);
//
//                insertNewTrailers(context, moviesCursor);
//                moviesCursor.moveToFirst();
//                insertNewReviews(context, moviesCursor);
//                moviesCursor.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static void addTrailersAndReviews(Context context, Movie movie) {
        String selection = PopularMovieContract.TrailerEntry.MOVIE_KEY + " = ? AND "
                + PopularMovieContract.TrailerEntry.REGISTRY_TYPE + " = ? ";
        String[] selectionArg = new String[]{Integer.toString(movie.getId()), movie.getRegistryType()};

        context.getContentResolver().delete(
                PopularMovieContract.TrailerEntry.CONTENT_URI,
                selection,
                selectionArg);

        insertTrailersFromMovie(context, movie);

        context.getContentResolver().delete(
                PopularMovieContract.ReviewEntry.CONTENT_URI,
                selection,
                selectionArg);

        insertReviewsFromMovie(context, movie);

    }


    private static void insertTrailersFromMovie(Context context, Movie movie) {
        ArrayList<Trailer> trailersList = NetworksUtils.getTrailers(movie.getMovieWebId());
        if (trailersList != null && trailersList.size() > 0) {
            ContentValues[] trailersContentValues = new ContentValues[trailersList.size()];
            for (int i = 0; i < trailersList.size(); i++) {
                trailersList.get(i).setMovieKey(movie.getId());
                trailersList.get(i).setRegistryType(movie.getRegistryType());
                trailersContentValues[i] = trailersList.get(i).getContentValues();
            }

            context.getContentResolver().bulkInsert(PopularMovieContract.TrailerEntry.CONTENT_URI,
                    trailersContentValues);
        }

    }

    private static void insertReviewsFromMovie(Context context, Movie movie) {
        ArrayList<Review> reviewList = NetworksUtils.getReviews(movie.getMovieWebId());
        if (reviewList != null && reviewList.size() > 0) {
            ContentValues[] reviewContentValues = new ContentValues[reviewList.size()];
            for (int i = 0; i < reviewList.size(); i++) {
                reviewList.get(i).setMovieKey(movie.getId());
                reviewList.get(i).setRegistryType(movie.getRegistryType());
                reviewContentValues[i] = reviewList.get(i).getContentValues();
            }

            context.getContentResolver().bulkInsert(PopularMovieContract.ReviewEntry.CONTENT_URI,
                    reviewContentValues);
        }

    }

//    private static void insertNewTrailers(Context context, Cursor moviesCursor) {
//
//        do {
//            int indexId = moviesCursor.getColumnIndex(PopularMovieContract.MovieEntry._ID);
//            int _id = moviesCursor.getInt(indexId);
//            int indexMovieId = moviesCursor.getColumnIndex(MOVIE_WEB_ID);
//            String movieId = moviesCursor.getString(indexMovieId);
//            int indexRegistryType = moviesCursor.getColumnIndex(PopularMovieContract.MovieEntry.REGISTRY_TYPE);
//            String registryType = moviesCursor.getString(indexRegistryType);
//
//            ArrayList<Trailer> trailersList = NetworksUtils.getTrailers(movieId);
//            if (trailersList != null && trailersList.size() > 0) {
//                ContentValues[] trailersContentValues = new ContentValues[trailersList.size()];
//                for (int i = 0; i < trailersList.size(); i++) {
//                    trailersList.get(i).setMovieKey(_id);
//                    trailersList.get(i).setRegistryType(registryType);
//                    trailersContentValues[i] = trailersList.get(i).getContentValues();
//                }
//
//                context.getContentResolver().bulkInsert(PopularMovieContract.TrailerEntry.CONTENT_URI,
//                        trailersContentValues);
//            }
//        } while (moviesCursor.moveToNext());
//    }


//    private static void insertNewReviews(Context context, Cursor moviesCursor) {
//
//        do {
//            int indexId = moviesCursor.getColumnIndex(PopularMovieContract.MovieEntry._ID);
//            int _id = moviesCursor.getInt(indexId);
//            int indexMovieId = moviesCursor.getColumnIndex(MOVIE_WEB_ID);
//            String movieId = moviesCursor.getString(indexMovieId);
//            int indexRegistryType = moviesCursor.getColumnIndex(PopularMovieContract.MovieEntry.REGISTRY_TYPE);
//            String registryType = moviesCursor.getString(indexRegistryType);
//
//            ArrayList<Review> reviewsList = NetworksUtils.getReviews(movieId);
//            if (reviewsList != null && reviewsList.size() > 0) {
//                ContentValues[] trailersContentValues = new ContentValues[reviewsList.size()];
//                for (int i = 0; i < reviewsList.size(); i++) {
//                    reviewsList.get(i).setMovieKey(_id);
//                    reviewsList.get(i).setRegistryType(registryType);
//                    trailersContentValues[i] = reviewsList.get(i).getContentValues();
//                }
//
//                context.getContentResolver().bulkInsert(PopularMovieContract.ReviewEntry.CONTENT_URI,
//                        trailersContentValues);
//            }
//        } while (moviesCursor.moveToNext());
//    }


}
