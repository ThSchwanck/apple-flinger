/*******************************************************************************
 * Copyright (C) 2015-2017 Andreas Redmer <andreasredmer@mailchuck.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.gitlab.ardash.appleflinger.i18n;

import java.io.UnsupportedEncodingException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Gdx;
import com.gitlab.ardash.appleflinger.helpers.Pref;

public class I18N {
	
	public enum k{
		gameName,startNewGame
	}

//	private static final String BUNDLE_NAME = "com.gitlab.ardash.appleflinger.i18n.af"; //$NON-NLS-1$

//	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	
	// the following line need to be in release, otherwise the class wont be found after optimization
	private static ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle (getBundleBaseName());

	static
	{
		loadLanguageBundle(Pref.getLingo());
	}
	
	private I18N() {
	}
	
	private static String getBundleBaseName()
	{
		switch (Gdx.app.getType()) {
		case Android:
//			return "af";
			return "assets/af";
		case Applet:
			return "";
		case Desktop:
			return "af";
//			return "assets/af";
		case HeadlessDesktop:
			return "assets/af";
		case WebGL:
			return "";
		case iOS:
			return "";
		}
		throw new RuntimeException("Unknown application type " + Gdx.app.getType());
	}
	
	public static void loadLanguageBundle(String languageCode)
	{
		Pref.setLingo(languageCode);
		String bundleName;
		if (languageCode.equals(""))
		{
			// set to system default
			bundleName = getBundleBaseName();
		}
		else
		{
			bundleName = getBundleBaseName()+"_"+languageCode;
		}
//		RESOURCE_BUNDLE = ResourceBundle.getBundle (bundleName);
		RESOURCE_BUNDLE = ResourceBundle.getBundle(bundleName,   
//	            new GdxFileControl("ISO-8859-1", FileType.Internal));
        new GdxFileControl("UTF-8", FileType.Classpath));
	}

	public static String getString(String key) {
		return s(key);
	}
	
	public static String s(String key) {
		try {
			String val = RESOURCE_BUNDLE.getString(key);
//			String s = new String(val.getBytes("ISO-8859-1"), "UTF-8");
//			return s;
			return val;
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		} 
//			catch (UnsupportedEncodingException e) {
//			throw new RuntimeException(e);
//		}
	}
	
	public static String g(k key) {
		return RESOURCE_BUNDLE.getString(key.name());
	}
}
