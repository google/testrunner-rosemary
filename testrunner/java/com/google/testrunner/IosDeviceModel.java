// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//    
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
    
package com.google.testrunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Data model of the different iOS devices.
 *
 * <p>Each model includes model name, product type, screen width and screen heights (both in
 * pixels).
 *
 * <p>The relationship between product type and model name is listed in
 * https://en.wikipedia.org/wiki/List_of_iOS_devices .
 */
public enum IosDeviceModel {
  IPHONE_1ST_GENERATION("iPhone 1st generation", "iphone1,1"),
  IPHONE_3("iPhone 3", "iphone1,2"),
  IPHONE_3GS("iPhone 3GS", "iphone2,1"),
  IPHONE_4("iPhone 4", "iphone3,1", "iphone3,2", "iphone3,3"),
  IPHONE_5C("iPhone 5C", "iphone5,3", "iphone5,4"),

  IPHONE_4S(640, 960, "iPhone 4s", "iphone4,1"),
  IPHONE_5(640, 1136, "iPhone 5", "iphone5,1", "iphone5,2"),
  IPHONE_5S(640, 1136, "iPhone 5s", "iphone6,1", "iphone6,2"),
  IPHONE_6_PLUS(1080, 1920, "iPhone 6 Plus", "iphone7,1"),
  IPHONE_6(750, 1334, "iPhone 6", "iphone7,2"),
  IPHONE_6S_PLUS(1080, 1920, "iPhone 6s Plus", "iphone8,2"),
  IPHONE_6S(750, 1334, "iPhone 6s", "iphone8,1"),
  IPHONE_SE(640, 1136, "iPhone SE", "iphone8,4"),
  IPHONE_7(750, 1334, "iPhone 7", "iphone9,1", "iphone9,3"),
  IPHONE_7_PLUS(1080, 1920, "iPhone 7 Plus", "iphone9,4", "iphone9,2"),
  RESIZABLE_IPHONE(768, 1024, "Resizable iPhone"),

  IPAD("iPad", "ipad1,1"),
  IPAD_3TH_GEN("iPad 3th gen", "ipad3,1", "ipad3,2", "ipad3,3"),
  IPAD_4TH_GEN("iPad 4th gen", "ipad3,4", "ipad3,5", "ipad3,6"),
  IPAD_PRO_9_7("iPad Pro(9.7 inch)", "ipad6,3", "ipad6,4"),
  IPAD_2017("iPad 2017", "ipad6,11", "ipad6,12"),

  IPAD_2(768, 1024, "iPad 2", "ipad2,1", "ipad2,2", "ipad2,3", "ipad2,4"),
  IPAD_RETINA(1536, 2048, "iPad Retina"),
  IPAD_AIR(1536, 2048, "iPad Air", "ipad4,1", "ipad4,2", "ipad4,3"),
  IPAD_AIR_2(1536, 2048, "iPad Air 2", "ipad5,3", "ipad5,4"),
  IPAD_PRO_12_9(2048, 2732, "iPad Pro", "ipad6,7", "ipad6,8"),
  RESIZABLE_IPAD(768, 1024, "Resizable iPad"),
  IDEVICE_GENERIC(640, 1136, "iDevice"),

  IPAD_MINI_1("iPad Mini 1", "ipad2,5", "ipad2,6", "ipad2,7"),
  IPAD_MINI_2("iPad Mini 2", "ipad4,4", "ipad4,5", "ipad4,6"),
  IPAD_MINI_3("iPad Mini 3", "ipad4,7", "ipad4,8", "ipad4,9"),
  IPAD_MINI_4("iPad Mini 4", "ipad5,1", "ipad5,2"),

  IPOD_1G("iPod Touch 1G", "ipod1,1"),
  IPOD_2G("iPod Touch 2G", "ipod2,1"),
  IPOD_3G("iPod Touch 3G", "ipod3,1"),
  IPOD_4G("iPod Touch 4G", "ipod4,1"),
  IPOD_5G("iPod Touch 5G", "ipod5,1"),
  IPOD_6G("iPod Touch 6G", "ipod7,1");

  /** The model name of the device. */
  private final String modelName;
  /** The screen width of the device in pixels. */
  private final int screenWidthInPixels;
  /** The screen height of the device in pixels. */
  private final int screenHeightInPixels;
  /** The list of the possible product type of the device. */
  // Not using ImmutibleList to keep the JAR file size small.
  @SuppressWarnings("ImmutableEnumChecker")
  private final List<String> productTypes;

  /** Constructor for the model used in both the real device and the simulator. */
  IosDeviceModel(
      int screenWidthInPixels, int screenHeightInPixels, String modelName, String... productTypes) {
    this.modelName = modelName;
    this.screenWidthInPixels = screenWidthInPixels;
    this.screenHeightInPixels = screenHeightInPixels;
    this.productTypes = new ArrayList<>(productTypes.length);
    for (String productType : productTypes) {
      this.productTypes.add(productType);
    }
  }

  /** Constructor for the model used only in the real device */
  IosDeviceModel(String modelName, String... productTypes) {
    this(0, 0, modelName, productTypes);
  }

  /** @return The screen width in pixels. 0 if the model is not applied in the simulator. */
  public int getScreenWidthInPixels() {
    return screenWidthInPixels;
  }

  /** @return The screen height in pixels. 0 if the model is not applied in the simulator. */
  public int getScreenHeightInPixels() {
    return screenHeightInPixels;
  }

  /** @return The simulator device type id. */
  public String getSimulatorDeviceTypeId() {
    return "com.apple.CoreSimulator.SimDeviceType." + modelName.replace(' ', '-');
  }
  /**
   * @param simulatorType the model name of the device.
   * @return The enum value for the given simulator type.
   * @throws NullPointerException If a null simulatorType is provided.
   * @throws IllegalArgumentException If there's no enum value for the specified simulator type.
   */
  public static IosDeviceModel valueForSimulatorType(String simulatorType) {
    simulatorType = simulatorType.replace(' ', '_').toUpperCase();
    if (simulatorType.equals("IDEVICE")) {
      simulatorType = simulatorType + "_GENERIC";
    } else if (simulatorType.equals("IPAD_PRO")) {
      simulatorType = simulatorType + "_12_9";
    }
    try {
      return IosDeviceModel.valueOf(simulatorType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("No device found for simulatorType: " + simulatorType);
    }
  }

  /**
   * Gets the model name of the device by the given product type.
   *
   * @param productType The product type.
   * @return The model name of the given product type.
   */
  public static String getModelNamebyProductType(String productType) {
    for (IosDeviceModel device : IosDeviceModel.class.getEnumConstants()) {
      for (String string : device.productTypes) {
        if (string.equals(productType)) {
          if (device == IosDeviceModel.IPAD_PRO_12_9) {
            return "iPad Pro (12.7 inches)";
          } else {
            return device.modelName;
          }
        }
      }
    }
    return productType + " unknown model";
  }
}
