/*
 * Copyright (C) 2006-2013 Bitronix Software (http://www.bitronix.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitronix.tm.utils;

import junit.framework.TestCase;

import java.util.Map;
import java.util.Properties;

/**
 * @author Ludovic Orban
 */
public class PropertyUtilsTest
		extends TestCase
{

	/**
	 * Method testSetProperties ...
	 */
	public void testSetProperties()
	{
		Destination destination = new Destination();

		PropertyUtils.setProperty(destination, "props.key", "value");
		assertEquals("value", destination.getProps()
		                                 .getProperty("key"));
		PropertyUtils.setProperty(destination, "subDestination.props.key", "value");
		assertEquals("value", destination.getSubDestination()
		                                 .getProps()
		                                 .getProperty("key"));
		PropertyUtils.setProperty(destination, "anInteger", "10");
		assertEquals(10, destination.getAnInteger());
		PropertyUtils.setProperty(destination, "subDestination.anInteger", "20");
		assertEquals(20, destination.getSubDestination()
		                            .getAnInteger());
		PropertyUtils.setProperty(destination, "aBoolean", "true");
		assertEquals(true, destination.isABoolean());
		PropertyUtils.setProperty(destination, "aWriteOnlyInt", "20");

		PrivateDestination privateDestination = new PrivateDestination();
		try
		{
			PropertyUtils.setProperty(privateDestination, "subDestination.props.key", "value");
			fail("it is not possible to set the 'subDestination' property, PropertyException should have been thrown");
		}
		catch (PropertyException ex)
		{
			assertEquals("cannot set property 'subDestination.props.key' - 'subDestination' is null and cannot be auto-filled", ex.getMessage());
		}
	}

	/**
	 * Method testSetMultipleProperties ...
	 */
	public void testSetMultipleProperties()
	{
		Destination destination = new Destination();

		PropertyUtils.setProperty(destination, "props.key1", "value1");
		PropertyUtils.setProperty(destination, "props.key2", "value2");
		assertEquals("value1", destination.getProps()
		                                  .getProperty("key1"));
		assertEquals("value2", destination.getProps()
		                                  .getProperty("key2"));
	}

	/**
	 * Method testSetClonedProperties ...
	 */
	public void testSetClonedProperties()
	{
		Destination destination = new Destination();

		PropertyUtils.setProperty(destination, "clonedProps.key", "value");
		assertEquals("value", destination.getClonedProps()
		                                 .getProperty("key"));
	}

	/**
	 * Method testSetMultipleClonedProperties ...
	 */
	public void testSetMultipleClonedProperties()
	{
		Destination destination = new Destination();

		PropertyUtils.setProperty(destination, "clonedProps.key1", "value1");
		PropertyUtils.setProperty(destination, "clonedProps.key2", "value2");
		assertEquals("value1", destination.getClonedProps()
		                                  .getProperty("key1"));
		assertEquals("value2", destination.getClonedProps()
		                                  .getProperty("key2"));
	}

	/**
	 * Method testSetPropertiesDirectly ...
	 */
	public void testSetPropertiesDirectly()
	{
		Destination destination = new Destination();

		Properties p = new Properties();
		p.setProperty("key", "value");

		PropertyUtils.setProperty(destination, "props", p);
		assertEquals("value", destination.getProps()
		                                 .getProperty("key"));
	}

	/**
	 * Method testSetClonedPropertiesDirectly ...
	 */
	public void testSetClonedPropertiesDirectly()
	{
		Destination destination = new Destination();

		Properties p = new Properties();
		p.setProperty("key", "value");

		PropertyUtils.setProperty(destination, "clonedProps", p);
		assertEquals("value", destination.getClonedProps()
		                                 .getProperty("key"));
	}

	/**
	 * Method testSettingKeyForPropertiesObject ...
	 */
	public void testSettingKeyForPropertiesObject()
	{
		Properties p = new Properties();
		PropertyUtils.setProperty(p, "key", "value");
		assertEquals("value", p.getProperty("key"));
	}

	/**
	 * Method testSetPropertiesObjectLongKey ...
	 */
	public void testSetPropertiesObjectLongKey()
	{
		PrivateDestination destination = new PrivateDestination();

		PropertyUtils.setProperty(destination, "props.key", "value1");
		PropertyUtils.setProperty(destination, "props.a.dotted.key", "value2");

		assertEquals("value1", destination.getProps()
		                                  .get("key"));
		assertEquals("value2", destination.getProps()
		                                  .get("a.dotted.key"));
	}

	/**
	 * Method testSmartGetProperties ...
	 */
	public void testSmartGetProperties()
	{
		Destination destination = new Destination();
		destination.setAnInteger(10);
		destination.setABoolean(true);

		Properties props = new Properties();
		props.setProperty("number1", "one");
		props.setProperty("number2", "two");
		destination.setProps(props);

		Map map = PropertyUtils.getProperties(destination);

		assertEquals(13, map.size());
		assertEquals("one", map.get("props.number1"));
		assertEquals("two", map.get("props.number2"));
		assertEquals(10, map.get("anInteger"));
		assertEquals(Boolean.TRUE, map.get("aBoolean"));
		assertEquals(Boolean.FALSE, map.get("anotherBoolean"));
		assertNull(map.get("subDestination"));
	}

	/**
	 * Method testSetPrimitiveTypes ...
	 */
	public void testSetPrimitiveTypes()
	{
		Destination destination = new Destination();

		PropertyUtils.setProperty(destination, "aString", "this is my string");
		PropertyUtils.setProperty(destination, "aBoolean", "true");
		PropertyUtils.setProperty(destination, "aByte", "100");
		PropertyUtils.setProperty(destination, "aShort", "20000");
		PropertyUtils.setProperty(destination, "anInteger", "300000");
		PropertyUtils.setProperty(destination, "aLong", "4000000");
		PropertyUtils.setProperty(destination, "aFloat", "3.14");
		PropertyUtils.setProperty(destination, "aDouble", "0.654987");

		assertEquals("this is my string", destination.getAString());
		assertEquals(true, destination.isABoolean());
		assertEquals(100, destination.getAByte());
		assertEquals(20000, destination.getAShort());
		assertEquals(300000, destination.getAnInteger());
		assertEquals(4000000, destination.getALong());
		assertEquals(3.14f, destination.getAFloat(), 0.01f);
		assertEquals(0.654987, destination.getADouble(), 0.000001);
	}

	/**
	 * Method testGetPrimitiveTypes ...
	 */
	public void testGetPrimitiveTypes()
	{
		Destination destination = new Destination();
		destination.setAString("this is my string");
		destination.setABoolean(true);
		destination.setAByte((byte) 100);
		destination.setAShort((short) 20000);
		destination.setAnInteger(300000);
		destination.setALong(4000000L);
		destination.setAFloat(3.14f);
		destination.setADouble(0.654987);

		assertEquals("this is my string", PropertyUtils.getProperty(destination, "aString"));
		assertEquals(Boolean.TRUE, PropertyUtils.getProperty(destination, "aBoolean"));
		assertEquals((byte) 100, PropertyUtils.getProperty(destination, "aByte"));
		assertEquals((short) 20000, PropertyUtils.getProperty(destination, "aShort"));
		assertEquals(300000, PropertyUtils.getProperty(destination, "anInteger"));
		assertEquals(4000000L, PropertyUtils.getProperty(destination, "aLong"));
		assertEquals(3.14f, PropertyUtils.getProperty(destination, "aFloat"));
		assertEquals(0.654987, PropertyUtils.getProperty(destination, "aDouble"));
	}

	public static class Destination
	{
		private Properties props;
		private Properties clonedProps;
		private Destination subDestination;
		private int anInteger;
		private int aWriteOnlyInt;
		private boolean aBoolean;
		private boolean anotherBoolean;
		private String aString;
		private byte aByte;
		private short aShort;
		private long aLong;
		private float aFloat;
		private double aDouble;

		/**
		 * Method getProps returns the props of this Destination object.
		 *
		 * @return the props (type Properties) of this Destination object.
		 */
		public Properties getProps()
		{
			return props;
		}

		/**
		 * Method setProps sets the props of this Destination object.
		 *
		 * @param props
		 * 		the props of this Destination object.
		 */
		public void setProps(Properties props)
		{
			this.props = props;
		}

		/**
		 * Method getClonedProps returns the clonedProps of this Destination object.
		 *
		 * @return the clonedProps (type Properties) of this Destination object.
		 */
		public Properties getClonedProps()
		{
			return (clonedProps == null) ? null : (Properties) clonedProps.clone();
		}

		/**
		 * Method setClonedProps sets the clonedProps of this Destination object.
		 *
		 * @param props
		 * 		the clonedProps of this Destination object.
		 */
		public void setClonedProps(Properties props)
		{
			clonedProps = (props == null) ? null : (Properties) props.clone();
		}

		/**
		 * Method getSubDestination returns the subDestination of this Destination object.
		 *
		 * @return the subDestination (type Destination) of this Destination object.
		 */
		public Destination getSubDestination()
		{
			return subDestination;
		}

		/**
		 * Method setSubDestination sets the subDestination of this Destination object.
		 *
		 * @param subDestination
		 * 		the subDestination of this Destination object.
		 */
		public void setSubDestination(Destination subDestination)
		{
			this.subDestination = subDestination;
		}

		/**
		 * Method getAnInteger returns the anInteger of this Destination object.
		 *
		 * @return the anInteger (type int) of this Destination object.
		 */
		public int getAnInteger()
		{
			return anInteger;
		}

		/**
		 * Method setAnInteger sets the anInteger of this Destination object.
		 *
		 * @param anInteger
		 * 		the anInteger of this Destination object.
		 */
		public void setAnInteger(int anInteger)
		{
			this.anInteger = anInteger;
		}

		/**
		 * Method setAWriteOnlyInt sets the AWriteOnlyInt of this Destination object.
		 *
		 * @param aWriteOnlyInt
		 * 		the AWriteOnlyInt of this Destination object.
		 */
		public void setAWriteOnlyInt(int aWriteOnlyInt)
		{
			this.aWriteOnlyInt = aWriteOnlyInt;
		}

		/**
		 * Method isABoolean returns the ABoolean of this Destination object.
		 *
		 * @return the ABoolean (type boolean) of this Destination object.
		 */
		public boolean isABoolean()
		{
			return aBoolean;
		}

		/**
		 * Method setABoolean sets the ABoolean of this Destination object.
		 *
		 * @param aBoolean
		 * 		the ABoolean of this Destination object.
		 */
		public void setABoolean(boolean aBoolean)
		{
			this.aBoolean = aBoolean;
		}

		/**
		 * Method isAnotherBoolean returns the anotherBoolean of this Destination object.
		 *
		 * @return the anotherBoolean (type boolean) of this Destination object.
		 */
		public boolean isAnotherBoolean()
		{
			return anotherBoolean;
		}

		/**
		 * Method setAnotherBoolean sets the anotherBoolean of this Destination object.
		 *
		 * @param anotherBoolean
		 * 		the anotherBoolean of this Destination object.
		 */
		public void setAnotherBoolean(boolean anotherBoolean)
		{
			this.anotherBoolean = anotherBoolean;
		}

		/**
		 * Method getAString returns the AString of this Destination object.
		 *
		 * @return the AString (type String) of this Destination object.
		 */
		public String getAString()
		{
			return aString;
		}

		/**
		 * Method setAString sets the AString of this Destination object.
		 *
		 * @param aString
		 * 		the AString of this Destination object.
		 */
		public void setAString(String aString)
		{
			this.aString = aString;
		}

		/**
		 * Method getAByte returns the AByte of this Destination object.
		 *
		 * @return the AByte (type byte) of this Destination object.
		 */
		public byte getAByte()
		{
			return aByte;
		}

		/**
		 * Method setAByte sets the AByte of this Destination object.
		 *
		 * @param aByte
		 * 		the AByte of this Destination object.
		 */
		public void setAByte(byte aByte)
		{
			this.aByte = aByte;
		}

		/**
		 * Method getAShort returns the AShort of this Destination object.
		 *
		 * @return the AShort (type short) of this Destination object.
		 */
		public short getAShort()
		{
			return aShort;
		}

		/**
		 * Method setAShort sets the AShort of this Destination object.
		 *
		 * @param aShort
		 * 		the AShort of this Destination object.
		 */
		public void setAShort(short aShort)
		{
			this.aShort = aShort;
		}

		/**
		 * Method getALong returns the ALong of this Destination object.
		 *
		 * @return the ALong (type long) of this Destination object.
		 */
		public long getALong()
		{
			return aLong;
		}

		/**
		 * Method setALong sets the ALong of this Destination object.
		 *
		 * @param aLong
		 * 		the ALong of this Destination object.
		 */
		public void setALong(long aLong)
		{
			this.aLong = aLong;
		}

		/**
		 * Method getAFloat returns the AFloat of this Destination object.
		 *
		 * @return the AFloat (type float) of this Destination object.
		 */
		public float getAFloat()
		{
			return aFloat;
		}

		/**
		 * Method setAFloat sets the AFloat of this Destination object.
		 *
		 * @param aFloat
		 * 		the AFloat of this Destination object.
		 */
		public void setAFloat(float aFloat)
		{
			this.aFloat = aFloat;
		}

		/**
		 * Method getADouble returns the ADouble of this Destination object.
		 *
		 * @return the ADouble (type double) of this Destination object.
		 */
		public double getADouble()
		{
			return aDouble;
		}

		/**
		 * Method setADouble sets the ADouble of this Destination object.
		 *
		 * @param aDouble
		 * 		the ADouble of this Destination object.
		 */
		public void setADouble(double aDouble)
		{
			this.aDouble = aDouble;
		}
	}

	private class PrivateDestination
	{
		private Properties props;
		private PrivateDestination subDestination;

		/**
		 * Method getProps returns the props of this PrivateDestination object.
		 *
		 * @return the props (type Properties) of this PrivateDestination object.
		 */
		public Properties getProps()
		{
			return props;
		}

		/**
		 * Method setProps sets the props of this PrivateDestination object.
		 *
		 * @param props
		 * 		the props of this PrivateDestination object.
		 */
		public void setProps(Properties props)
		{
			this.props = props;
		}

		/**
		 * Method getSubDestination returns the subDestination of this PrivateDestination object.
		 *
		 * @return the subDestination (type PrivateDestination) of this PrivateDestination object.
		 */
		public PrivateDestination getSubDestination()
		{
			return subDestination;
		}

		/**
		 * Method setSubDestination sets the subDestination of this PrivateDestination object.
		 *
		 * @param subDestination
		 * 		the subDestination of this PrivateDestination object.
		 */
		public void setSubDestination(PrivateDestination subDestination)
		{
			this.subDestination = subDestination;
		}
	}

}
