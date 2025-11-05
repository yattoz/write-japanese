require 'nokogiri'

doc = Nokogiri::XML(File.open("JMDict.xml")) do |config|
  config.strict.noblanks
end
