package com.threshold.toolbox;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks;
import android.content.res.Configuration;
import android.util.DisplayMetrics;

/**
 * 像素密度适配
 *
 * <p> px：(pixels)（像素）：屏幕上的点，表示屏幕实际的象素，与密度相关。密度大了，单位面积上的px会比较多。
 *         例如，720*1080的屏幕在横向有720个像素，在纵向有1080像素。</p>
 * <p> dp(dip):密度无关像素（Density Independent Pixels），这个和硬件设备有关，
 *             一般给view设置大小的时候用这个单位，在不同分辨率的设备上会有不同的缩放感觉。</p>
 * <p> dpi：屏幕像素密度：每英寸上的像素点数（dot per inch）density：dp与px的比值，即density = dp/px;
 *          Android规定 density = dpi/160，即当dpi为160时：1dp=1px。</p>
 * <p> scaledDensity:字体的缩放因子，正常情况下和density相等，但是调节系统字体大小后会改变这个值。</p>
 *
 * <p>像素密度适配原理：由于不同的设备density、scaledDensity的值不同，
 *    同一分辨率的设备的density、scaledDensity的值也可能不一样。
 *    所以我们可以通过修改density、scaledDensity、densityDpi的值来直接更改系统内部对于目标尺寸而言的像素密度。</p>
 *
 * <code>
 *     public class BaseActivity extends AppcompatActivity {
 *
 *         protected void onCreate(Bundle savedInstanceState) {
 *             super.onCreate(savedInstanceState);
 *             Density.setupDensity(getApplication(), this);//call this before setContentView
 *             //setContentView(R.layout.activity_main);
 *         }
 *
 *     }
 * </code>
 *
 * <code>
 * <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:tools="http://schemas.android.com/tools"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     tools:context=".MainActivity">
 *
 *     <TextView
 *         android:id="@+id/tv1"
 *         android:layout_width="160dp"
 *         android:layout_height="160dp"
 *         android:background="@color/colorPrimary"
 *         android:text="Hello World1!" />
 *
 *     <TextView
 *         android:id="@+id/tv2"
 *         android:layout_width="160dp"
 *         android:layout_height="160dp"
 *         android:layout_below="@+id/tv1"
 *         android:layout_toRightOf="@+id/tv1"
 *         android:background="@color/colorPrimary"
 *         android:text="Hello World2!" />
 *
 * </RelativeLayout>
 * </code>
 *
 */
public class DensityUtil {

    //  use iphone5(4inch)(1136x640)(xhdpi/320dpi(ppi))(density=2) as standard dimens
    //  measure element pixel in picture and let it divide 2 to as it dp.
    //   for example, a pic width is 320px, height is 400px, you should write 160dp as width, 200dp as height on dimens.xml,
    //   because of its density is 2, so android will calculate xhdpi width to 320px, xhdpi height to 400px
    private static final float WIDTH = 320; //参考设备的宽，单位dp
    private static float appDensity = 0; //表示屏幕密度
    private static float appScaledDensity = 0;//表示字体缩放比例,默认与appDensity相等

    public static void setupDensity(final Application application, Activity activity) {
        //get application screen display info
        final DisplayMetrics displayMetrics = application.getResources().getDisplayMetrics();
        if (appDensity < 1) {
            appDensity = displayMetrics.density;
            appScaledDensity = displayMetrics.scaledDensity;

            //add listener for text font size changed
            application.registerComponentCallbacks(new ComponentCallbacks() {
                @Override
                public void onConfigurationChanged(Configuration newConfig) {
                    if (newConfig != null && newConfig.fontScale > 0) { //说明字体大小改变了
                        //重新赋值appScaledDensity
                        appScaledDensity = application.getResources().getDisplayMetrics().scaledDensity;
                    }
                }

                @Override
                public void onLowMemory() {

                }
            });
        }

        //calculate target density/scaledDensity/densityDpi
        final float targetDensity = displayMetrics.widthPixels / WIDTH;
        final float targetScaledDensity = targetDensity * (appDensity / appScaledDensity);
        final int targetDensityDpi = (int) (targetDensity * 160);

        //replace density/scaledDensity/densityDpi of activity
        final DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        dm.density = targetDensity;
        dm.scaledDensity = targetScaledDensity;
        dm.densityDpi = targetDensityDpi;
    }
}

