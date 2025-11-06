require 'nokogiri'
require 'pry'
doc = Nokogiri::XML(File.open("JMDictAssetsBuilder/JMDict.xml")) do |config|
  config.strict.noblanks
end

entries = doc.xpath("//entry")
nicechar = '之'
selected = []
entries.each do |unit|
  k_ele = unit.css("k_ele")
  if !k_ele.empty?
    keb = k_ele.css("keb")
      if !keb.empty?
        if keb.first.text.include?(nicechar)
           selected.append(unit)
           puts "Found #{keb.first.text}"
        end
      end
  end
end
puts selected.size




