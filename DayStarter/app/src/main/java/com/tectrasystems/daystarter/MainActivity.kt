package com.tectrasystems.daystarter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.*
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.DisplayMetrics
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //check if stop button on notification is pressed
        if (intent.hasExtra("stopNotification")) {
            //disable notification
            notifBox.isChecked = false
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("notifCheck", notifBox.isChecked).apply()
            hideNotification()
            finishAffinity()
        }

        //date and time loop
        val mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(object : Runnable {
            override fun run() {
                //get date and time every 500 milliseconds
                timeLoop()
                dateLoop()
                mainHandler.postDelayed(this, 500)
            }
        })

        //get check status
        notifBox.isChecked = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean("notifCheck", false)

        //get intention text
        intentionTxt.setText(
            PreferenceManager.getDefaultSharedPreferences(this)
                .getString("intentionString", "")
        )

        //get image
        getImage()

        //get a random quote
        getQuote()

        //start animations
        animations()

        //when intentionTxt text is changed
        intentionTextChanged()

        //when intentionTxt key is pressed
        intentionKeyPress()

        //when refresh button pressed
        refreshBtnPress()

        //when notification box pressed
        notifBoxPress()

        //image category button press
        imageCategoryPress()

        //when app info button pressed
        appInfoBtnPress()

        if (notifBox.isChecked) {
            //save check preference
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("notifCheck", notifBox.isChecked).apply()

            //show notification and multitasking notice
            showNotification()
        }

        //update quotes widget
        updateQuotesWidget()

        //handle save button click
        saveBtn.setOnClickListener { updateIntention() }
    }

    private fun appInfoBtnPress() {
        //app info button press
        appInfoBtn.setOnClickListener {
            // build alert dialog
            val pInfo: PackageInfo =
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0)
            val version: String = pInfo.versionName

            val dialogBuilder = AlertDialog.Builder(this).create()

            // set message of alert dialog and make OK button
            val message =
                SpannableString("Created and designed by John Spahr.\nhttps://johnspahr.org\n\nVersion $version\n\nImages from unsplash.com (via Unsplash Source)\n\nOpen source libraries:\n\nGlide - github.com/bumptech/glide")
            Linkify.addLinks(message, Linkify.WEB_URLS)
            dialogBuilder.setMessage(message)
            dialogBuilder.setButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE,
                "OK"
            ) { dialog, _ ->
                dialog.dismiss()
            }

            dialogBuilder.setTitle("about binoc")

            // show alert dialog
            dialogBuilder.show()

            dialogBuilder.apply {
                //handle link clicks
                findViewById<TextView>(android.R.id.message)
                    ?.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun imageCategoryPress() {
        //image category button press
        imageCategoryBtn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("image category")
                .setPositiveButton("Cancel", null)
                .setItems(
                    R.array.imageCategories
                ) { _, which ->
                    //get image categories from array
                    val categories = resources.getStringArray(R.array.imageCategories)
                    val imageCategory = categories[which]

                    //show user that image category has been chosen
                    val toast: Toast = Toast.makeText(
                        applicationContext,
                        "🖼 image category set to: $imageCategory",
                        Toast.LENGTH_SHORT
                    )
                    toast.show()

                    //set button text
                    imageCategoryBtn.text = "category: $imageCategory"

                    //save image category preference
                    PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("imageCategory", imageCategory).apply()

                }
            //create and show category picker
            builder.create()
            builder.show()
        }
    }

    private fun notifBoxPress() {
        notifBox.setOnClickListener {
            //save check preference
            updateIntention()

            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("notifCheck", notifBox.isChecked).apply()

            if (notifBox.isChecked) {
                if (intentionTxt.text.isNotBlank()) {
                    //intention has been inputted, show notification and notice
                    showNotification()
                }
            } else {
                //hide notification and notice
                hideNotification()
            }
        }
    }

    private fun refreshBtnPress() {
        refreshBtn.setOnClickListener {
            //show toast
            val toast: Toast = Toast.makeText(
                applicationContext,
                "🔃 refreshing...",
                Toast.LENGTH_SHORT
            )
            toast.show()

            //refresh image and quote
            getImage()
            getQuote()

            //scroll to 0, 0
            scrollView.scrollTo(0, 0)
        }
    }

    private fun intentionTextChanged() {
        //when text changed
        intentionTxt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //show intentionNotice
                intentionNotice.visibility = View.VISIBLE
            }
        })
    }

    private fun intentionKeyPress() {
        //when intentionTxt enter key pressed
        intentionTxt.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER || actionId == EditorInfo.IME_ACTION_DONE) {
                //when done button is pressed, save value and show notification (if enabled)
                updateIntention()
            }
            false
        }
    }

    private fun updateIntention() {
        PreferenceManager.getDefaultSharedPreferences(this).edit()
            .putString("intentionString", intentionTxt.text.toString()).apply()

        //if persistent notification box is checked...
        if (notifBox.isChecked) {
            //show notification
            showNotification()
        }

        //hide intentionNotice
        intentionNotice.visibility = View.GONE

        updateIntentionWidget()

        //show toast indicating that the new intention has been saved
        val toast: Toast = Toast.makeText(
            applicationContext,
            "💾 intention has been saved",
            Toast.LENGTH_SHORT
        )
        toast.show()
    }

    private fun updateIntentionWidget() {
        //update intention widget
        val appWidgetManager =
            AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                this,
                IntentionWidgetProvider::class.java
            )
        )
        if (appWidgetIds.isNotEmpty()) {
            IntentionWidgetProvider().onUpdate(this, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateQuotesWidget() {
        val appWidgetManager =
            AppWidgetManager.getInstance(applicationContext)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(
                this,
                QuotesWidgetProvider::class.java
            )
        )
        if (appWidgetIds.isNotEmpty()) {
            QuotesWidgetProvider().onUpdate(this, appWidgetManager, appWidgetIds)
        }
    }

    private fun hideNotification() {
        val mContext = applicationContext
        val notificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        //get rid of notification
        notificationManager.cancel(1)
    }

    private fun showNotification() {
        //show notification and create intent that opens app for when notification is clicked
        if (intentionTxt.text.isNotBlank()) {
            val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val notificationStopIntent = Intent(this@MainActivity, MainActivity::class.java)
            notificationStopIntent.putExtra("stopNotification", true)
            val pendingStopIntent = PendingIntent.getActivity(
                this@MainActivity, 0, notificationStopIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            //notification builder
            val builder = NotificationCompat.Builder(this, "all_notifications")
                .setContentTitle("Your Intention")
                .setContentText("\"" + intentionTxt.text + "\"")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setColor(1)
                .setContentIntent(pendingIntent)
                .setAutoCancel(false)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", pendingStopIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "all_notifications"
                val mChannel = NotificationChannel(
                    channelId,
                    "Intention Notification",
                    NotificationManager.IMPORTANCE_DEFAULT
                )

                //create notification channel description
                mChannel.description = "Intention Notification Channel"

                //create application context value
                val mContext = applicationContext

                //create notification manager
                val notificationManager =
                    mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }

            //show notification and set id to 1
            with(NotificationManagerCompat.from(this)) {
                notify(1, builder.build())
            }
        } else {
            //if invalid intention
            notifBox.isChecked = false

            //show error dialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Error")
            builder.setMessage("Please enter a valid intention to enable persistent notifications.")
            builder.setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getQuote() {
        //get array
        val quotes = resources.getStringArray(R.array.quotes)

        //pick random quote
        val randomQuote = kotlin.random.Random.Default.nextInt(quotes.size - 1)
        val selectedQuote = quotes[randomQuote]

        //split text and author
        val quoteArray: List<String> = selectedQuote.split("@")

        //set text
        quoteTxt.text = '"' + quoteArray[0] + '"' //quote
        quoteAuthorTxt.text = " - " + quoteArray[1] //author
    }

    private fun timeLoop() {
        //get time
        val time = SimpleDateFormat("hh\nmm", Locale.US)
        timeTxt.text = time.format(Date())
    }

    private fun dateLoop() {
        //get date
        val date = SimpleDateFormat("E,\nMM/dd/yy", Locale.US)
        dateTxt.text = date.format(Date())
    }

    private fun animations() {
        //set animation types
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        //show layout and fade in
        mainLayout.visibility = View.VISIBLE
        mainLayout.startAnimation(fadeIn)
    }

    @SuppressLint("SetTextI18n")
    private fun getImage() {
        //get screen size
        val displayMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.getRealMetrics(displayMetrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(displayMetrics)
        }

        //get width and height of screen
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        //get image category preference
        val imageCategory = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("imageCategory", "nature")

        //set image category button text
        imageCategoryBtn.text = "category: $imageCategory"

        if (imageCategory != "no images") {
            //get random image from Unsplash
            Glide.with(this)
                .load("https://source.unsplash.com/random/" + width + "x" + height + "/?" + imageCategory)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .transition(withCrossFade(500))
                .into(backgroundImg)
        } else {
            //hide image
            backgroundImg.setImageDrawable(null)
        }
    }
}
