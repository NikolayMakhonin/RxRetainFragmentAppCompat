package com.github.rovkinmax.rxretainexample

import android.app.Activity
import android.app.FragmentManager
import android.content.Context
import android.content.Intent
import android.support.test.InstrumentationRegistry
import android.support.test.filters.SdkSuppress
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import com.github.rovkinmax.rxretain.RxRetainFactory
import com.github.rovkinmax.rxretainexample.activity.TestableActivity
import com.robotium.solo.Solo
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import rx.Observable
import rx.observers.TestSubscriber
import java.util.concurrent.TimeUnit.SECONDS

/**
 * @author Rovkin Max
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 18)
class RecreationActivityTest {

    @get:Rule
    val rule = ActivityTestRule<TestableActivity>(TestableActivity::class.java, false, false)

    private lateinit var fragmentManager: FragmentManager
    private lateinit var activity: Activity
    private lateinit var device: UiDevice
    private lateinit var context: Context
    private lateinit var basePackage: String
    private lateinit var solo: Solo
    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.setOrientationNatural()
        device.unfreezeRotation()

        context = InstrumentationRegistry.getContext()
        activity = rule.launchActivity(Intent(context, TestableActivity::class.java))
        solo = Solo(InstrumentationRegistry.getInstrumentation(), activity)
        basePackage = activity.packageName
        fragmentManager = activity.fragmentManager
    }

    @Test
    fun testUnsubscribeAfterActivityRotation() {
        val testSubscriber = TestSubscriber<Long>()
        RxRetainFactory.start(fragmentManager, Observable.timer(10, SECONDS).bindToThread(), testSubscriber)
        testSubscriber.awaitTerminalEvent(2, SECONDS)

        changeOrientationAndWait()
        testSubscriber.assertUnsubscribed()
    }

    @Test
    fun testSubscribeAfterRotation() {
        val testSubscriber = TestSubscriber<Int>()
        RxRetainFactory.start(fragmentManager, rangeWithDelay(0, 10, SECONDS.toMillis(10)).bindToThread(), testSubscriber)
        testSubscriber.awaitTerminalEvent(2, SECONDS)

        changeOrientationAndWait()
        testSubscriber.assertUnsubscribed()

        val secondSubscriber = TestSubscriber<Int>()
        RxRetainFactory.start(fragmentManager, rangeWithDelay(0, 10, SECONDS.toMillis(15)), secondSubscriber)

        if (!secondSubscriber.isUnsubscribed)
            secondSubscriber.awaitTerminalEvent()

        secondSubscriber.assertCompleted()
        secondSubscriber.assertReceivedOnNext((0..9).toArrayList())
    }

    private fun changeOrientationAndWait() {
        device.setOrientationLeft()
        device.waitForWindowUpdate(basePackage, SECONDS.toMillis(5))
    }

    @After
    fun tearDown() {
        solo.finishOpenedActivities()
    }
}