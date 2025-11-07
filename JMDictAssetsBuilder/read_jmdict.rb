require 'nokogiri'
require 'pry'

require_relative './detect_empty_trans.rb'

MAX_SCORE = 1e3

def get_commonness(priority)
  # explanations on commonness: https://jedict.com/HTML/edict_doc.html
  case priority
  when "news1", "gai1"; 23
  when "ichi1"; 20
  when "spec1"; 25
  when "news2", "gai2"; 50
  when "spec2"; 45
  when "ichi2"; 70
  when /nf\d+/;
    # can be an nfXX string. XX corresonds to the bracket of 500 words
    # the word belongs to. Example: nf23 => 23 * 500 = 11500 (between 11000 and 11500)
    # Incidentally, keeping this number works reasonably well with the above scoring.
    commonness_bracket = Integer(priority.match(/\d+/).to_s.gsub(/(^0*)/, ""))
    commonness_bracket
  else MAX_SCORE
  end
end

def get_best_score(priority_list)
    if (priority_list.empty?)
        return MAX_SCORE
    end
    return (priority_list.map { |unit| get_commonness(unit) }).min
end

def get_entry_score(entry)
  first_word = entry.xpath(".//keb").first
  priority_list = first_word.xpath("..//ke_pri").map {|unit| "#{unit.text}" }
  score = get_best_score(priority_list)
  puts "#{first_word.text}\t#{priority_list}\t#{score}"
  return score
end


doc = Nokogiri::XML(File.open("JMDictAssetsBuilder/JMDict.xml")) do |config|
  config.strict.noblanks
end


# display first 10, or N
MAX_SCORE_OF_EXAMPLES = 15

def generate_trans_xml(nicechar, dictionary)
    
  entries = dictionary.xpath("//entry")
  selected = []
  entries.each do |unit|
    k_ele = unit.css("k_ele")
    if !k_ele.empty?
      keb = k_ele.css("keb")
        if !keb.empty?
          if keb.first.text.include?(nicechar)
            selected.append(unit)
            # puts "Found #{keb.first.text}"
            get_entry_score(unit)
          end
        end
    end
  end

  puts selected.size

  sorted = selected.sort_by { |unit|
    get_entry_score(unit)
  }

  # It may happen that there is literally no example.
  # In this case, we craft a dummy example.
  if sorted.size < 1
    empty_doc = Nokogiri::XML(File.open("JMDictAssetsBuilder/empty_trans.xml")) do |config|
      config.strict.noblanks
    end
    entries = empty_doc.xpath("//entry")
    sorted.append(entries.first)
    puts "added dummy entry"
  end

  number_of_examples = [MAX_SCORE_OF_EXAMPLES, sorted.size].min
  
  raise "size error" unless sorted.size > 0

  for i in 0..(number_of_examples - 1) do
      puts [i, number_of_examples]
      priority_list = sorted[i].xpath(".//keb").first.xpath("..//ke_pri").map {|unit| "#{unit.text}" }
      puts "#{i}\t#{sorted[i].xpath(".//keb").first.text}   ---   #{priority_list} --> #{get_best_score(priority_list)}"
  end

  sorted_best = sorted[0, number_of_examples]

  # clean up
  # 1. remove Entity Reference tags with &...;
  # It's brutal, but I don't know how basic the parser in write-japanese is... so better remove all I see...
  sorted_best.each do |entry|
    entry.xpath(".//pos").each do |entref|
      entref.content = entref.child.name
    end
    entry.xpath(".//ke_inf").each do |entref|
      entref.content = entref.child.name
    end
    entry.xpath(".//re_inf").each do |entref|
      entref.content = entref.child.name
    end
    entry.xpath(".//misc").each do |entref|
      entref.content = entref.child.name
    end
  end

  def write_output(nicehex, entries)

    filename = "JMDictAssetsBuilder/output_assets/#{nicehex}_trans.xml"
    # filename = "app/src/main/assets/char_vocab/#{nicehex}_trans.xml"
    if File.exist?(filename)  
      File.delete(filename)
    end
    
    output_file = File.open(filename, "w:UTF-8")
    document = Nokogiri::XML::Builder.new(:encoding => 'UTF-8') { |xml|
      xml.JMdict()
    }.doc
    
    root_node = document.child
    for entry in entries do
      root_node.add_child(entry)
    end
    # minify the XML document. This is a TERRIBLE way of doing, but for now it works. I couldn't find better.
    minified = document.to_xml.gsub("\n", "").gsub(/>\s+</, "><")
    # And write!
    output_file.write(minified)
    output_file.close()
  end

  write_output(gethex(nicechar), sorted_best)
end

# generate_trans_xml(getkanji("4e1e"), doc)

get_problematic_trans().each { |kanji| generate_trans_xml(kanji, doc) }
