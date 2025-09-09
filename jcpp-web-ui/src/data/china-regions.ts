/*
 * 开源代码，仅供学习和交流研究使用，商用请联系三丙
 * 微信：mohan_88888
 * 抖音：程序员三丙
 * 付费课程知识星球：https://t.zsxq.com/aKtXo
 */

// 基于 china-division 包和 afc163 示例代码实现的省市区级联数据
// 参考：https://gist.github.com/afc163/7582f35654fd03d5be7009444345ea17

const provinces = require('china-division/dist/provinces.json');
const cities = require('china-division/dist/cities.json');
const areas = require('china-division/dist/areas.json');

export interface RegionOption {
  label: string;
  value: string;
  children?: RegionOption[];
}

// 区县数据接口
interface Area {
  code: string;
  name: string;
  cityCode: string;
}

// 城市数据接口
interface City {
  code: string;
  name: string;
  provinceCode: string;
  children?: RegionOption[];
}

// 省份数据接口
interface Province {
  code: string;
  name: string;
  children?: RegionOption[];
}

// 类型转换
const typedAreas = areas as Area[];
const typedCities = cities as City[];
const typedProvinces = provinces as Province[];

// 为每个城市添加区县数据
typedAreas.forEach((area) => {
  const matchCity = typedCities.find(city => city.code === area.cityCode);
  if (matchCity) {
    matchCity.children = matchCity.children || [];
    matchCity.children.push({
      label: area.name,
      value: area.code,
    });
  }
});

// 为每个省份添加城市数据
typedCities.forEach((city) => {
  const matchProvince = typedProvinces.find(province => province.code === city.provinceCode);
  if (matchProvince) {
    matchProvince.children = matchProvince.children || [];
    matchProvince.children.push({
      label: city.name,
      value: city.code,
      children: city.children,
    });
  }
});

// 构建最终的级联选择器数据
export const chinaRegions: RegionOption[] = typedProvinces.map(province => ({
  label: province.name,
  value: province.code,
  children: province.children,
}));
