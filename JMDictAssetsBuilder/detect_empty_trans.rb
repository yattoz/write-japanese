
require 'nokogiri'

# get hex value from kanji (char)
def gethex(c)
  return c.ord.to_s(16)
end

# get kanji (char) from hex string
def getkanji(h)
  return h.to_i(16).chr(Encoding::UTF_8)    
end

def get_problematic_trans()
  selected = []
  translation_files = Dir.glob("./app/src/main/assets/char_vocab/*.xml")
  translation_files.filter do |unit|
    nicehex = unit.gsub(/^.*char_vocab\//, "").gsub("_trans.xml", "")
    translation_to_check = Nokogiri::XML(File.open(unit)) do |config|
      config.recover
    end
    puts "file: #{unit} - #{translation_to_check.errors}" if !translation_to_check.errors.empty?
    if translation_to_check.child.children.size < 1
      puts "Found empty translation file for: #{getkanji(nicehex)}"
      selected.append(getkanji(nicehex))
    elsif !translation_to_check.errors.empty?
      puts "Found malformed XML for: #{getkanji(nicehex)}"
      selected.append(getkanji(nicehex))
    end
  end
  return selected
end

puts get_problematic_trans().size