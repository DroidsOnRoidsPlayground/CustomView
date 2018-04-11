package pl.droidsonrioids.customview

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        android1.setOnClickListener { android1.limitToBounds = !android1.limitToBounds }
        val random = Random()
        android2.setOnClickListener {
            android2.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            android2.androidWidth *= 2
        }
    }
}
