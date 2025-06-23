package com.patikprojects.justineai.assist


import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.patikprojects.justineai.activity.BaseActivity

class JustineAssistantActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val sheet = JustineAssistantBottomSheet()
            sheet.show(supportFragmentManager, "JustineAssistantSheet")
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(object :
            FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewDestroyed(fm: FragmentManager, f: Fragment) {
                if (f is JustineAssistantBottomSheet && !isFinishing && !isChangingConfigurations) {
                    finish()
                    fm.unregisterFragmentLifecycleCallbacks(this)
                }
            }
        }, false)
    }
}