/*
The MIT License

Copyright (c) 2010 Matt Kane

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Code taken from: https://github.com/wildabeast/BarcodeScanner
*/
package no.nordicsemi.android;

import android.app.Activity;
import android.content.Context;

import java.lang.reflect.Field;

/**
 * R replacement for PhoneGap Build.
 *
 * ([^.\w])R\.(\w+)\.(\w+)
 * $1fakeR("$2", "$3")
 *
 * @author Maciej Nux Jaros
 */
public class FakeR {
	private Context context;
	private String packageName;

	public FakeR(Activity activity) {
		context = activity.getApplicationContext();
		packageName = context.getPackageName();
	}

	public FakeR(Context context) {
		this.context = context;
		packageName = context.getPackageName();
	}

	public int getId(String group, String key) {
		return context.getResources().getIdentifier(key, group, packageName);
	}

	public static int getId(Context context, String group, String key) {
		return context.getResources().getIdentifier(key, group, context.getPackageName());
	}

	/**
	 * Gets a field from the project's BuildConfig. This is useful when, for example, flavors
	 * are used at the project level to set custom fields.
	 * @param context       Used to find the correct file
	 * @param fieldName     The name of the field-to-access
	 * @return              The value of the field, or {@code null} if the field is not found.
	 */
	public Object getBuildConfigValue(String fieldName) {
		try {
			Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
			Field field = clazz.getField(fieldName);
			return field.get(null);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
