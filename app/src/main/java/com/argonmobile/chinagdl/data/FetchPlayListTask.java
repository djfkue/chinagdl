/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.argonmobile.chinagdl.data;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class FetchPlayListTask extends AsyncTask<Void, Void, Boolean> {

    private final String LOG_TAG = FetchPlayListTask.class.getSimpleName();

    private final String BASE_URL =
            "http://www.njgdg.org/playlist.php";

    private static ObjectMapper sObjectMapper = new ObjectMapper();

    private ArrayList<PlayListItem> mPlayLists = new ArrayList<PlayListItem>();

    private PlayList mPlayList;

    private OnFetchPlayListListener mOnFetchPlayListListener;

    public FetchPlayListTask() {
    }

    public void setOnFetchPlayListListener(OnFetchPlayListListener onFetchPlayListListener) {
        mOnFetchPlayListListener = onFetchPlayListListener;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private void getVideoDataFromJson(String videoListJsonStr) throws IOException {

        PlayList playList = sObjectMapper.readValue(videoListJsonStr, PlayList.class);
        Log.v(LOG_TAG, "Get videos: " + playList.playList.size());
        mPlayList = playList;

    }

    @Override
    protected void onPreExecute() {
        if (mOnFetchPlayListListener != null) {
            mOnFetchPlayListListener.onFetchStart();
        }
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Log.v(LOG_TAG, "do in background .................");
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String videoListJsonStr = null;

        try {

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return false;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return false;
            }

            videoListJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }

        try {
            getVideoDataFromJson(videoListJsonStr);
            return true;
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        // This will only happen if there was an error getting or parsing the forecast.
        return false;
    }

    // This is called when doInBackground() is finished
    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            mPlayLists.clear();

            mPlayLists.addAll(mPlayList.playList);
            Log.v(LOG_TAG, "onPostExecute Get playlists: " + mPlayLists.size());
            if (mOnFetchPlayListListener != null) {
                mOnFetchPlayListListener.onFetchSucceed(mPlayLists);
            }
        } else {
            if (mOnFetchPlayListListener != null) {
                mOnFetchPlayListListener.onFetchFailed();
            }
        }
    }

    @Override
    protected void onCancelled(Boolean result) {
        if (mOnFetchPlayListListener != null) {
            mOnFetchPlayListListener.onFetchCancelled();
        }
    }

    public interface OnFetchPlayListListener {
        public void onFetchStart();
        public void onFetchFailed();
        public void onFetchCancelled();
        public void onFetchSucceed(ArrayList<PlayListItem> playLists);
    }
}
