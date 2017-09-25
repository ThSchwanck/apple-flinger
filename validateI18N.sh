error_file=/tmp/i18nerrors.log
rm -f $error_file
touch $error_file
in_source=`egrep -a -r "I18N.getString|I18N.s" core/ | egrep -a -o "I18N.getString.*)|I18N.s.*)" | sed "s/I18N/\nI18N/g" | egrep -a -o "I18N.getString.*\")|I18N.s.*\")" | egrep -a -o '".*"' | sort -u | egrep -o "[a-zA-Z0-9_]*"`
prop_files=`ls android/assets/af*properties`

# check string in java code that do not come up in prop files
echo these are i18n strings in the source code
echo $in_source
echo 
echo checking all of them
echo 

for ins in $in_source
do
	echo checking: $ins

	for pro in $prop_files
	do
		echo in prop file: $pro
		NUM=`egrep -a "^$ins=" $pro | wc -l`
		[[ $NUM -eq "1" ]] || echo ERROR $ins is in Java code but not in $pro | tee -a $error_file
	done
done

# check strings in prop files, that do not come up in java code
# they must be removed to avoid useless translation load for future property files
echo 
echo checking values in prop files that are not used in java source code
for pro in $prop_files
do
	echo in prop file: $pro
	in_this_prop=`egrep -a -o "^.*=" $pro | tr -d '='`

	for fin in $in_this_prop
	do
		echo searching for uses of: $fin
		NUM=`egrep -a -r "I18N.getString\(\"$fin\"\)|I18N.s\(\"$fin\"\)" core/ | wc -l`
		echo occurences of $fin: $NUM 
		[[ $NUM -ge "1" ]] || echo ERROR $fin in $pro does not occur in any java code | tee -a $error_file
	done

#	[[ $NUM -eq $number_of_ins ]] || echo ERROR $pro contains $NUM lines, but in java code there are $number_of_ins references | tee -a $error_file
done


# double check all the numbers
echo 
echo checking number of lines in prop files
number_of_ins=`echo $in_source | wc -w`
for pro in $prop_files
do
	echo in prop file: $pro
	NUM=`egrep -a "^.*=" $pro | wc -l`
	[[ $NUM -eq $number_of_ins ]] || echo ERROR $pro contains $NUM lines, but in java code there are $number_of_ins references | tee -a $error_file
done

echo 

error_count=`cat $error_file | wc -l`
echo 
echo found $error_count errors:
cat $error_file
exit $error_count

