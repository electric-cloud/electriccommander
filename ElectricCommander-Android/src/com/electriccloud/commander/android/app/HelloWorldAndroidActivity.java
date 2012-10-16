/*
 * Copyright (C) 2012 Electric Cloud, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.electriccloud.commander.android.app;

import com.electriccloud.commander.android.app.R;

import android.app.Activity;

import android.content.Intent;

import android.os.Bundle;

import android.view.View;

import android.widget.EditText;

public class HelloWorldAndroidActivity
    extends Activity
{

    //~ Static fields/initializers ---------------------------------------------

    public static final String USER_NAME = "com.commander.android.Username";
    public static final String SERVER    = "com.commander.android.Server";
    public static final String PASSWORD  = "com.commander.android.Password";

    //~ Methods ----------------------------------------------------------------

    /**
     * Called when the activity is first created.
     *
     * @param  savedInstanceState
     */
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    /**
     * Called when the user selects the Send button.
     *
     * @param  view
     */
    public void sendMessage(View view)
    {

        // Do something in response to button
        Intent intent = new Intent(this, NetworkActivity.class);

        // User name
        EditText editText = (EditText) findViewById(R.id.uname);

        intent.putExtra(USER_NAME, editText.getText()
                                           .toString());
        editText = (EditText) findViewById(R.id.server);
        intent.putExtra(SERVER, editText.getText()
                                        .toString());
        editText = (EditText) findViewById(R.id.pass);
        intent.putExtra(PASSWORD, editText.getText()
                                          .toString());
        startActivity(intent);
    }
}
